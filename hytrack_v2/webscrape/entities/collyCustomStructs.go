package entities

import (
	"net/url"
)

type ResultTuple struct {
	URL        *url.URL
	ResultCode int
	Body       []byte
}

type ResultInformation struct {
	Endpoint  string
	Results   []ResultTuple
	Redirects []RedirectTuple
}

type RedirectTuple struct {
	Host              string
	RequestedEndpoint string
	Via               []string
}

type CrawlMeta struct {
	StartTime string
	EndTime   string
	TopList   string
	Duration  string
}

type RankingTuple struct {
	Index int
	Url   string
}
