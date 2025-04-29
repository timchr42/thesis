package de.tubs.cs.ias.plotalyzer

import de.halcony.plotalyzer.database.entities.InterfaceAnalysis
import de.halcony.plotalyzer.database.entities.trafficcollection.{Cookie, Header, Request, Response}
import de.halcony.plotalyzer.plugins.{AnalysisContext, AnalysisPlugin, AnalysisReturn, JSONReturn}
import spray.json.{JsArray, JsBoolean, JsNumber, JsObject, JsString, JsValue}
import wvlet.log.LogSupport

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration.Inf
import scala.concurrent.{Await, Future}

/** Plugin to analyze the misses where objection+mitmproxy were unable to intercept HTTPS
  *
  * @author Simon Koch
  *
  */
class CustomTabTracking() extends AnalysisPlugin with LogSupport {

  private val ANALYSIS_DESCRIPTION = "Simple Traffic Collection"

  /**
   * {
   *  _start : <startOfAnalysis>,
   *  _success : <bool>,
   *  _requests : <count>,
   *  _requests_success : <count>,
   *  requests : [{
   *          "_domain:  : "<domain>",
   *          "_start: : <timeOfRequest>,
   *          "_success" : <count>,
   *          "_path" : <string>,
   *          "cookies: {
   *              <string> : <string>,
   *              ...
   *          },
   *          "header": {
   *             <string> : <string>,
   *             ...
   *          },
   *          responses : [{
   *             "_success" : <bool>,
   *             "_status" : <int>,
   *             "_start" : <timeOfResponse>,
   *             "cookies" : {
   *                  <string> : <string>,
   *                  ...
   *             },
   *             "headers" : {
   *                <string> : <string>,
   *                ....
   *             }
   *          },...]
   *     }, ...]
   *  }
   *
   * @param success whether the app analysis was successful
   * @param requests the list of requests observed during the analysis
   *
   */
  private case class AppResult(success : Boolean, requests : List[Request], start : ZonedDateTime) {

    def getRequestCount : Int = requests.length

    def getRequestSuccessCount : Int = requests.count(_.error.isEmpty)

    private def getCookieJson(cookies: List[Cookie]) : JsObject = {
      JsObject(
        cookies.map(cookie => cookie.name -> JsString(cookie.values)).toMap
      )
    }

    private def getHeaderJson(headers: List[Header]) : JsObject = {
      JsObject(
        headers.map(header => header.name -> JsString(header.values)).toMap
      )
    }

    private def getResponseJson(response : Response) : JsObject = {
      val ret = JsObject(
        "_success" -> JsBoolean(response.error.isEmpty),
        "_status" -> JsNumber(response.statusCode),
        "_start" -> JsNumber(response.startTime.toEpochSecond),
        "cookies" -> getCookieJson(response.cookies),
        "headers" -> getHeaderJson(response.header),
      )
      ret.prettyPrint
      ret
    }

    private def getResponsesJson(responses: List[Response]) : JsArray = {
      JsArray(
        responses.map(getResponseJson).toVector
      )
    }

    private def getRequestJson(request: Request): JsObject = {
      val pathAndQuery = if (request.getPathWithQuery != null) request.getPathWithQuery else ""
      val host = if (request.host != null) request.host else "N/A"
      val ret = JsObject(
        "_domain" -> JsString(host),
        "_path" -> JsString(pathAndQuery),
        "_success" -> JsBoolean(request.error.isEmpty),
        "_start" -> JsNumber(request.start.toEpochSecond),
        "cookies" -> getCookieJson(request.cookies),
        "header" -> getHeaderJson(request.headers),
        "responses" -> getResponsesJson(request.response),
      )
      //ret.prettyPrint
      ret
    }

    private def getRequestsJson : JsArray = {
      JsArray(
        //WARN: this filter needs to be adapted based on the measurement timeframe of interest
            requests.filter(req => ChronoUnit.SECONDS.between(start, req.start) <= 1*60).map(getRequestJson).toVector
          )
    }

    def getJson: JsValue = {
      val ret = JsObject(
        "_start" -> JsNumber(start.toEpochSecond),
        "_success" -> JsBoolean(success),
        "_requests" -> JsNumber(getRequestCount),
        "_requests_success" -> JsNumber(getRequestSuccessCount),
        "requests" -> getRequestsJson
      )
      ret.prettyPrint
      ret
    }

  }

  /** create a JsObject that pretty prints to the summary
   *
   * {
   *    "apps" : <numberApps>,
   *    "apps_success" : <numberAppsSuccess>,
   *    "requests" : <numberRequests>,
   *    "requests_success" : <numberRequestsSuccess>,
   *    "requests_with_cookies" : <numberRequestsWithCookies>,
   *    "responses_with_cookies" : <numberResponsesWithCookies>
   * }
   *
   * @param apps the list of final app analysis summarizing their ssl interception rate
   * @return the created JsObject representing the summary
   */
  private def createSummary(apps: List[AppResult]): JsObject = {
    val ret = JsObject(
      "apps" -> JsNumber(apps.length),
      //the amount of apps which run without issue
      "apps_success" -> JsNumber(apps.count(_.success)),
      // the amount of requests monitored
      "requests" -> JsNumber(apps.map(_.requests.length).sum),
      // the amount of requests successfully intercepted
      "requests_success" -> JsNumber(apps.map(_.requests.count(_.error.isEmpty)).sum),
      // the amount of requests with cookies
      "requests_with_cookies" -> JsNumber(apps.flatMap(_.requests.map(_.cookies.nonEmpty)).count(_ == true)),
      // the amount of responses with cookies
      "responses_with_cookies" -> JsNumber(apps.flatMap(_.requests.flatMap(_.response.map(_.cookies.nonEmpty))).count(_ == true))
    )
    ret.prettyPrint
    ret
  }

  private def processApps(analyses: List[InterfaceAnalysis]): Map[String, AppResult] = {
    val future = Future.sequence {
      analyses.map {
        analysis =>
          Future { analysis.getApp.id -> AppResult(analysis.getSuccess, analysis.getTrafficCollection.flatMap(_.getRequests), analysis.getStart) }
      }
    }
    Await.result(future,Inf).toMap
  }

  /** returns an JSONReturn analysis result that pretty prints to
    * {
    *    "_experiment" : <id>,
    *    "_summary" : <resultSummary>,
    *    "apps" {
    *      "<appid>" : <AppResult>,
    *       ....
    *    }
    * }
    *
    *
    */
  override def analyze(
      context: AnalysisContext): Either[Exception, AnalysisReturn] = {
    val appAnalysis: Map[String, AppResult] = processApps(
      InterfaceAnalysis
        .get(context.experiment)(context.database)
        .filter { ana =>
          context.only match {
            case Some(only) => only.contains(ana.getApp.id)
            case None       => true
          }
        }
        .filter(_.getDescription == ANALYSIS_DESCRIPTION)
        .groupBy(_.getApp.id)
        .map(_._2.head)
        .toList)
    try {
      val json = JsObject(
        "_experiment" -> JsNumber(context.experiment.getId),
        "_summary" -> createSummary(appAnalysis.values.toList),
        "apps" -> JsObject(
          appAnalysis.map {
            case (app, ana) =>
              val buff = ana.getJson
              assert(buff != null)
              app -> buff
          }
        )
      )
      Right(JSONReturn(json))
    } catch {
      case x: Exception => Left(x)
    }
  }

}
