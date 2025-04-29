package de.halcony.appanalyzer

import de.halcony.appanalyzer.analysis.interaction.{Interface, InterfaceElementInteraction}
import de.halcony.appanalyzer.analysis.plugin.ActorPlugin
import de.halcony.appanalyzer.platform.appium.Appium
import de.halcony.appanalyzer.analysis.Analysis
import wvlet.log.LogSupport
import scala.io.StdIn.readLine

class TrafficCollection() extends ActorPlugin with LogSupport {

  private var timespan : Option[Long] = None

  override def setParameter(parameter: Map[String, String]): ActorPlugin = {
    timespan = Some(parameter.getOrElse("time-ms","60000").toLong)
    this
  }

  override def getDescription: String = "Simple Traffic Collection"

  override def action(interface: Interface)(implicit context: Analysis, appium: Appium): Option[InterfaceElementInteraction] = {
    context.checkIfAppIsStillRunning(true)
    if (timespan.get == -1) {
      info("starting infinity monitoring")
      readLine("press enter to stop.")
    } else {
      info(s"waiting for ${timespan.get} ms")
      Thread.sleep(timespan.get)
    }
    // wait for the specified time
    context.checkIfAppIsStillRunning(true)
    None // tell the analysis that you are done
  }

  /** check if actor wants to run again on the same app
   *
   * the first element indicates if the actor wants to run on the same app again
   * the second element indicates if the app should be reset before restarting
   *
   * @return
   */
  override def restartApp: (Boolean, Boolean) = {
    (false,false)
  }

  override def onAppStartup(implicit context: Analysis): Unit = {
    context.startTrafficCollection(None,"Simple Traffic Collection")
  }
}
