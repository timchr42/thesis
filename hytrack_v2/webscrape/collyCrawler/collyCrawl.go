package collyCrawler

import (
	"crypto/md5"
	"fmt"
	"log"
	"net/http"
	"strings"
	"time"
	"wellKnownCrawler/entities"

	"github.com/gocolly/colly/v2"
)

func valueInSlice(value int, list []int) bool {
	for _, listValue := range list {
		if listValue == value {
			return true
		}
	}
	return false
}

func Visit(endpoint string, websiteSlice []entities.RankingTuple, protocolHash map[[16]byte][]int, combinations []string) entities.ResultInformation {

	acceptableHttpCodes := []int{200, 202, 203, 206,
		300, 301, 302, 303, 304, 401, 403, 500}

	var foundEndpoints []entities.ResultTuple
	var redirectedEndpoints []entities.RedirectTuple
	c := colly.NewCollector(colly.Async(true))
	c.SetRequestTimeout(time.Second * 3)
	err := c.Limit(&colly.LimitRule{
		DomainGlob:  "*",
		Parallelism: 50,
	})
	if err != nil {
		log.Print(err)
	}
	c.OnRequest(func(r *colly.Request) {
		// fmt.Println(r)
		//r.Headers.Set("Referer", "http://YOUR-INSTUTION.example.org/")
		//r.Headers.Set("XYZ-Research-Project", "wellKnownCrawler")
	})
	c.SetRedirectHandler(func(req *http.Request, via []*http.Request) error {
		host := req.URL.Host
		reqEndpoint := req.URL.Path
		var viaPath []string
		for _, v := range via {
			if v.Host != host {
				viaPath = append(viaPath, fmt.Sprintf("%s%s", v.Host, v.URL.Path))
			} else {
				viaPath = append(viaPath, v.URL.Path)
			}
		}
		redirectedEndpoints = append(redirectedEndpoints, entities.RedirectTuple{
			Host:              host,
			RequestedEndpoint: reqEndpoint,
			Via:               viaPath,
		})
		return nil
	})

	endpointSplit := strings.Split(endpoint, "/")
	endpointName := endpointSplit[len(endpointSplit)-1]

	c.OnResponse(func(r *colly.Response) {
		if valueInSlice(r.StatusCode, acceptableHttpCodes) && strings.Contains(r.Request.URL.Path, endpointName) {
			foundEndpoints = append(foundEndpoints, entities.ResultTuple{
				URL:        r.Request.URL,
				ResultCode: r.StatusCode,
				Body:       r.Body,
			})
		}
	})

	for _, currentWebsite := range websiteSlice {
		var visitUrl string
		if currentWebsite.Url == "" {
			continue
		}
		if val, ok := protocolHash[md5.Sum([]byte(currentWebsite.Url))]; ok {
			for i, prefix := range combinations {
				if !valueInSlice(i, val) { // check if HEAD request was successful
					continue
				}
				if endpoint != "" {
					visitUrl = currentWebsite.Url + "/" + endpoint
				} else {
					visitUrl = currentWebsite.Url
				}
				err := c.Visit(prefix + visitUrl)
				if err != nil {
					log.Print(err)
				}
			}
		}
	}

	if endpoint == "" {
		endpoint = "websites"
	}
	c.Wait()
	return entities.ResultInformation{
		Endpoint:  endpoint,
		Results:   foundEndpoints,
		Redirects: redirectedEndpoints,
	}

}
