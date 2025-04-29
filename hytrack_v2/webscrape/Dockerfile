FROM golang:1.23
ADD collyCrawler /proj/collyCrawler
ADD entities /proj/entities
ADD wellKnownCrawlerMain /proj/wellKnownCrawlerMain
ADD go.mod go.sum /proj/
WORKDIR /proj/wellKnownCrawlerMain
RUN go build main.go
ENTRYPOINT [ "./main" ]