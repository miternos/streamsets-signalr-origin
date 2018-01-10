# streamsets-signalr-origin

Streamsets origin example for SignalR interface

How to use
==========
- Build the project with maven
- Install the fat jar (-with-dependencies) into stream sets as an external jar
- Restart StreamSets and you are ready to use custom components in pipeline

How it works
============
Gets a list of id's from an odata interface. Starts observe relation with signalr interface for each id. Then sends observe request to rest interface. When signalr client receives data, it is cached and sent to pipeline as a record. Each record will be as a map of name-value pairs. Names are whatever is sent from SignalR.

Ids are updated periodically, and on each update registration to rest is updated



