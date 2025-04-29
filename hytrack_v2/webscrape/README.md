# wellKnownCrawler

This is a generic endpoint crawler written in Go.
We configured it to scrape digital asset links, i.e. the `.well-known/assetlinks.json`.
As input it takes a list of endpoints and a list of domains.

## Structure

- *collyCrawl.go* does the actual visiting
- *collyCustomStructs.go* contains struct declarations used by the crawler
- *main.go* contains the main routine

## Installation

### Docker
1. Build image: `docker build . -t wk-crawler:latest` in this folder
2. Run: `docker run -v $PWD/resources:/resources wk-crawler:latest -endpointsFilePath=/resources/dal.txt -websitesFilePath=/resources/test_top-5000.csv` in this folder
   - specify another `websitesFilePath` to define the list of domains
   - specify another `endpointsFilePath` to redefine the endpoints to scrape
3. Copy the results out of the docker container: `docker cp $containerName:/proj/wellKnownCrawlerMain/crawl_results results`
   - you can find the container name via `docker container ls -a`

### Manually

1. install with `go build main.go` in the *wellKnownCrawlerMain* folder
2. this should have produced a *main* executable (optional change name with `-o`)
3. check with `./main -h` if the program works
4. now specify the endpoints to visit and the websites to visit with their respective program argument flags,
   -  E.g. to search for `.well-known/assetlinks.json`: `./main -endpointsFilePath=../resources/dal.txt -websitesFilePath=../resources/test_top-5000.csv`
5. the crawler creates a directory called "crawl_results" upon completion that contain the results of the crawl

  

## Additional Information

1. Be aware that the crawler is capable of creating a high amount of requests, change the **[Parallelism](https://go-colly.org/docs/examples/parallel/)** in the *collyCrawl.go* to adjust it to your needs
2. Uncomment Colly's *OnRequest* setting in the *main.go* and the *collyCrawl.go* to communicate in what context the scraping occurs to website owners
   - We recommend setting the Referer to a project-specific page hosted at your institution and setting a custom header with the project's name.
   - Adjust to your needs.
3. The website list should have the format `<rankingIndex>,<domainName>` WITHOUT any protocol prefix (a fitting list is the Tranco top list)
4. Note, that if an endpoint starts with a dot, i.e. ".well-known", the "crawl_results" directory will contain HIDDEN folders, as the crawler creates directories named like the endpoints
5. the endpoint and website CSV are specified relative to the directory the program is called from