# Plotalyzer Plugin: CustomTabTracking

This is the plugin required to extract the required raw data from the database.

## Build

Go into the root folder of this project and run:

```
$> sbt package
```

## Use

After building the plugin move the corresponding jar into the `plugin` folder of the Plotalyzer.
The plugin is then available for selection from the command line interface and can be run using.
`<id>` signifies the id of the experiment with the relevant traffic collection in the database generated
by the Appanalyzer. The `output.json` will then contain the data to identify possible cookie intersections.

```
$> ./run.sh analysis <id> /path/to/output.json CustomTabTracking
```