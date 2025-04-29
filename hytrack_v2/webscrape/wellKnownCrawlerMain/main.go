package main

import (
	"bufio"
	"crypto/md5"
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"math/rand"
	"os"
	"strconv"
	"strings"
	"sync"
	"time"
	"wellKnownCrawler/collyCrawler"
	"wellKnownCrawler/entities"

	"github.com/gocolly/colly/v2"
)

var combinations []string = []string{"https://www.", "https://"} // TWA DAL's exist only on https:

type safeProtocolHash struct {
	mu           sync.Mutex
	protocolHash map[[16]byte][]int
}

func main() {
	startTime := time.Now()
	safeProtHash := safeProtocolHash{protocolHash: make(map[[16]byte][]int)}
	endpointsToVisit := flag.String("endpointsFilePath", "", "specify the endpoints the crawler should visit")
	websitesToVisit := flag.String("websitesFilePath", "", "specify the websites the crawler should visit")
	flag.Parse()
	var endpoints []string

	if *endpointsToVisit != "" {
		file, err := os.Open(*endpointsToVisit)
		if err != nil {
			log.Fatal(err)
		}
		defer func(file *os.File) {
			err := file.Close()
			if err != nil {
				log.Fatal(err)
			}
		}(file)

		scanner := bufio.NewScanner(file)
		for scanner.Scan() {
			endpoints = append(endpoints, scanner.Text())
		}

		if err := scanner.Err(); err != nil {
			log.Fatal(err)
		}
	} else {
		endpoints = []string{""}
	}
	fmt.Println("----------Start Scraping----------")
	clearResultsDirectory()
	webSiteSlice := openWebsiteList(*websitesToVisit, 1000)
	numberOfEndpoints := len(endpoints)
	createProtocolHashList(webSiteSlice, &safeProtHash)
	fmt.Println("----------Finished Preflight----------")

	for sliceNumber, subSlice := range webSiteSlice {
		var wg sync.WaitGroup
		var wgWriteback sync.WaitGroup
		wg.Add(numberOfEndpoints)
		wgWriteback.Add(numberOfEndpoints)
		resultChannel := make(chan entities.ResultInformation)
		for _, endpoint := range endpoints {
			go func(endpoint string, co chan<- entities.ResultInformation) {
				defer wg.Done()
				co <- collyCrawler.Visit(endpoint, randomizeSlice(subSlice), safeProtHash.protocolHash, combinations)
			}(endpoint, resultChannel)
		}
		for i := 1; i <= numberOfEndpoints; i++ {
			tempResult := <-resultChannel
			go func(tempResult entities.ResultInformation) {
				defer wgWriteback.Done()
				fmt.Println("Writing back ")
				writeToFile(tempResult.Endpoint, tempResult.Results, "endpointInformation/", sliceNumber)
				writeToFile(tempResult.Endpoint, tempResult.Redirects, "redirectInformation/", sliceNumber)
			}(tempResult)
		}
		wg.Wait()
		wgWriteback.Wait()
		close(resultChannel)
	}

	fmt.Println("--------Finished Scraping---------")
	endTimeFormat := time.Now().Format("2006-01-02 15:04:05")
	startTimeFormat := startTime.Format("2006-01-02 15:04:05")
	duration := time.Since(startTime)
	completePathTopList, err := os.Getwd()
	checkPanic(err)
	completePathTopList += "/" + *websitesToVisit
	crawlMeta := entities.CrawlMeta{
		StartTime: startTimeFormat,
		EndTime:   endTimeFormat,
		TopList:   completePathTopList,
		Duration:  duration.Round(time.Second).String(),
	}
	writeToFile("crawlMeta", crawlMeta, "crawlMeta/", 0)
}

func createProtocolHashList(allWebsiteSlices [][]entities.RankingTuple, safeProtHash *safeProtocolHash) {
	c := colly.NewCollector(colly.Async(true))
	c.SetRequestTimeout(time.Millisecond * 2000)
	c.OnRequest(func(r *colly.Request) {
		//r.Headers.Set("Referer", "http://YOUR-INSTUTION.example.org/")
		//r.Headers.Set("XYZ-Research-Project", "wellKnownCrawler")
	})
	c.OnResponse(func(r *colly.Response) {
		if r.StatusCode == 200 {
			index, err := strconv.Atoi(r.Ctx.Get("index"))

			if err != nil {
				fmt.Println("Error during conversion")
				return
			}
			safeProtHash.mu.Lock()
			url := r.Ctx.Get("urlFromRanking")
			if url != "" {
				hash := md5.Sum([]byte(url))
				safeProtHash.protocolHash[hash] = append(safeProtHash.protocolHash[hash], index)
			}
			safeProtHash.mu.Unlock()
		}
	})
	err := c.Limit(&colly.LimitRule{
		DomainGlob:  "*",
		Parallelism: 50,
	})
	if err != nil {
		log.Print(err)
		return
	}
	for _, websiteSlice := range allWebsiteSlices {

		for _, rankingTuple := range websiteSlice {
			if rankingTuple.Url != "" {
				for index, prot := range combinations {
					requestContext := colly.NewContext()
					requestContext.Put("index", strconv.Itoa(index))
					requestContext.Put("urlFromRanking", rankingTuple.Url)
					err := c.Request("HEAD", prot+rankingTuple.Url, nil, requestContext, nil)
					if err != nil {
						continue
					}
				}
			}
		}
	}
	c.Wait()
}

func clearResultsDirectory() {
	path, err := os.Getwd()
	if err != nil {
		log.Fatal(err)
	}
	path += "/crawl_results/"
	if _, err := os.Stat(path); os.IsNotExist(err) {
		return
	}
	err = os.RemoveAll(path)
	if err != nil {
		log.Fatal(err)
	}
}

func writeToFile(endpoint string, data interface{}, subPath string, sliceNumber int) {
	var file *os.File
	path, err := os.Getwd()
	path += "/crawl_results/" + subPath
	if err != nil {
		log.Fatal(err)
	}

	endpointSplit := strings.Split(endpoint, "/")
	endpoint = endpointSplit[len(endpointSplit)-1]
	endpointPath := strings.Join(endpointSplit[:(len(endpointSplit)-1)], "/")
	if endpointPath != "" {
		path += endpointPath + "/"
	}
	path += endpoint + "/"
	err = os.MkdirAll(path, os.ModePerm)
	if err != nil {
		log.Println(err)
	}
	log.Println(fmt.Sprintf("%s%s", path, endpoint))

	file, err = os.Create(fmt.Sprintf("%s/%s_%d.json", path, endpoint, sliceNumber))

	if err != nil {
		log.Fatal(err)
	}

	var content []byte
	switch data.(type) {
	case []entities.ResultTuple, []entities.RedirectTuple, entities.CrawlMeta:
		content, err = json.MarshalIndent(data, "", " ")
		if err != nil {
			fmt.Println(err)
		}
		if content == nil {
			return
		}
	default:
		fmt.Println("Could not write " + endpoint + ".")
		return
	}

	defer func(file *os.File) {
		err := file.Close()
		if err != nil {
			log.Fatal(err)
		}
	}(file)

	_, err = file.Write(content)

	if err != nil {
		log.Fatal(err)
	}
}

func openWebsiteList(websitesToVisit string, sliceSize int) [][]entities.RankingTuple {
	var allSlices [][]entities.RankingTuple
	file, err := os.Open(websitesToVisit)
	if err != nil {
		log.Fatal(err)
	}
	defer func(file *os.File) {
		err := file.Close()
		if err != nil {
			log.Fatal(err)
		}
	}(file)
	scanner := bufio.NewScanner(file)
	currentSliceIndex := 0
	runListIndex := 0
	allSlices = append(allSlices, make([]entities.RankingTuple, sliceSize))
	for scanner.Scan() {
		var currentWebsite entities.RankingTuple
		websiteInfo := strings.Split(scanner.Text(), ",")
		index, err := strconv.Atoi(websiteInfo[0])
		checkPanic(err)
		currentWebsite = entities.RankingTuple{
			Index: index,
			Url:   websiteInfo[1],
		}
		allSlices[currentSliceIndex][runListIndex] = currentWebsite
		runListIndex += 1
		if runListIndex == sliceSize {
			runListIndex = 0
			currentSliceIndex += 1
			allSlices = append(allSlices, make([]entities.RankingTuple, sliceSize))
		}
	}
	return allSlices
}
func checkPanic(err error) {
	if err != nil {
		panic(err)
	}
}

func randomizeSlice(slice []entities.RankingTuple) []entities.RankingTuple {
	rand.Seed(time.Now().UnixNano())
	for i := range slice {
		j := rand.Intn(i + 1)
		slice[i], slice[j] = slice[j], slice[i]
	}

	return slice
}
