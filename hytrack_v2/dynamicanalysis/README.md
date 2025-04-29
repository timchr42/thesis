# Dynamic App Analysis Pipeline

As described in the paper, we use the established dynamic app analysis pipeline from our previous work *The OK Is Not Enough: A Large Scale Study of Consent Dialogs in Smartphone Applications*, Koch et al. We modified it to also record responses to requests. These changes are already upstream at https://github.com/App-Analysis

Refer to the original paper and artifact appendix for more information: https://www.usenix.org/conference/usenixsecurity23/presentation/koch

This artifact documents the state of the pipeline for this work and is intended for the Availability and Functionality Artifact Evaluation.


## Structure

- `Appanalyzer`: the main analysis program
 - `Plugin-TrafficCollection`: The app analyzer plugin required for the traffic collection, i.e. measurement
- `Plotalyzer`: Evaluation of app analyzer's results
 - `Plugin-CustomTabTracking`: This plugin obtains the data from the database to be analyzed by the Plotalyzer

Start with the README of the Appanalyzer.
