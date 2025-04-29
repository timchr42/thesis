# README

This is the artifact for our USENIX SECURITY 2025 Paper "HyTrack: Resurrectable and Persistent Tracking Across Android Apps and the Web".
We propose a new mobile tracking technique and search for it in the wild.

## Structure of the Artifact
The artifact is divided into subfolders:

- `demo`: Contains a demo video of HyTrack in action, as well as a description of the demo.
- `PoC`: Contains the proof-of-concept implementation of HyTrack: Two Android apps and a web server.
- `webscrape`: Contains our webscraper to scrape `assetlinks.json` files from the web.
- `dynamicanalysis`: Contains our pipeline for the dynamic app network analysis.
- `staticanalysis`: Contains our pipeline for the static app analysis.

## Usage
Refer to the READMEs in the respective subfolders.

## Cite us

```bibtex
@inproceedings{USENIX:Wessels:2025,
	title        = {HyTrack: Resurrectable and Persistent Tracking Across Android Apps and the Web},
	author       = {Malte Wessels and Simon Koch and Jan Drescher and Louis Bettels and David Klein and Martin Johns},
	year         = 2025,
	month        = aug,
	booktitle    = {34th USENIX Security Symposium (USENIX Security 25)},
	publisher    = {USENIX Association},
	address      = {Seattle, WA}
}
```
