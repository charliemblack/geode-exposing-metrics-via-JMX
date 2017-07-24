## Exposing Geode Statistics as JMX

Geode currently exposes tons of metrics via JMX.  However there are still hundreds more that could be exposed.   In this project we will expose some of those metrics via JMX so the information can be consumed in many ways.   

##  What does a statistics definition look like?

To get access to all of the metrics Geode exposes there is a tool that displays the metrics file.    We can see that tool in action below.

![vsd](/images/vsd.png)

The documentation on how to view the files:

https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=61309918

##  How do we specify a metric for display

For the example I thought it would be good to show a customizable example.   In this example I make use of regular expressions to make it easier to expose metrics in bulk.

To define which metrics to expose I used a plain ole properties file.   The format of the property and value is:

```
<Metric Type>:[Optional Resource RegEx |]<Metric RegEx>[,<Subsequent Metric RegEx> ]
```

Example:
```
VMMemoryPoolStats: .*Survivor.*|current.*,foo
```
In the above example *VMMemoryPoolStats* is the metric type.   

For the property value we have the metric resource and the metric regular expressions separated via the "|" char.   

If the pipe symbol isn't there then the parser will treat the metric resource name as the regular expression ".\*"

The metric regular expressions are a comma separated list.

That means for the VMMemoryPoolStats the app is going to expose all of the resource have the word Survivor spaces and only the metrics that start with current and the statistic foo.

I am sure there are going to be problems with some regular expressions and how the parser looks for commas.   So feel free to change up the implementation to meet the needs of your application.

## Project example with output

Lets see this in action!

For this example I am interested in all of the VMStats and some of the "Client Cache Server Statistics".  Namely I am interested in any statistic that starts with current and acceptsInProgress.

expose_metrics.properties:
```
VMStats: .*
VMMemoryPoolStats: .*Survivor.*|current.*
CacheServerStats: current.*,acceptsInProgress
```
Java Mission Control of process:
![vsd](/images/jmc.png)
