# Appanalyzer Plugin: Trafficcollection

This plugin activates the traffic collection either for a given time frame or an unlimited amount of time until human interaction.


## Build

In the root project folder run

```
$> sbt package
```


## Use

Move the create jar file into the configured `plugin` folder of the [Appanalyzer](https://github.com/App-Analysis/scala-appanalyzer).
Then the plugin is available for selection from the command line as a plugin to run app analysis with.
The default configuration conducts a 60second traffic collection, however, passing the parameter time-ms via the appanalyzer can adjust that value with `-1` being infinite and requiring human interaction to stop.
