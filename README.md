# Description 

The application extracts entities and relations from [Wikidata](https://www.wikidata.org/wiki/Wikidata:Main_Page) json dump file and sinks into Elasticsearch like my the [other application](https://github.com/Rende/wd-kb-extractor) does. Obviously the job can be done via one-thread application. However we wanted to explore that:

 - Can Apache Flink map reduce framework be used for this purpose? 
 - Would it improve the execution time?
 
Answers: Yes we can use Flink but the performance is variable since we can always change the settings (mostly depending on the machine power). However as a general observation we can say that reading json entries from one source and distributing over parallel jobs doesn't show improvement because the source function becomes bottleneck. We couldn't read the compressed file in parallel because [Flink doesn't support it](https://ci.apache.org/projects/flink/flink-docs-release-1.3/dev/batch/index.html#read-compressed-files) so we had to implement our own source function therefore we faced the problem. We could get away from this with a more intelligent dataset distribution model like splitting json dump file and distributing the chunks to the machines so that they can extract their own json entries, don't have to wait the main source function. Nevertheless Apache Flink shows good performance for data streams. You can find more information about Flink use-cases [here](https://flink.apache.org/usecases.html).

# Requirements
For Flink we need working Maven 3.0.4 (or higher) and Java 7.x (or higher) installations. 

download flink-1.1.4

    - wget "https://archive.apache.org/dist/flink/flink-1.1.4/flink-1.1.4-bin-hadoop1-scala_2.10.tgz"
	- tar -xvf flink-1.1.4-bin-hadoop2-scala_2.10.tgz
	- cd flink-1.1.4/conf/
	- nano flink-conf.yaml
		important! each slot is assigned to one thread e.g. 
    if your task will run in 2 threads and all must be run on the same machine, 
    set it as 2 => taskmanager.numberOfTaskSlots: 2
		save and exit
	we will use flink locally so no need to update master and slave files.
		
Since we use Flink  1.1.4 version in the project, we should use Elasticsearch 2.2.1 which is compatible with the Flink's ES connector.

download elasticsearch-2.2.1

    - wget "https://download.elasticsearch.org/elasticsearch/release/org/elasticsearch/distribution/tar/elasticsearch/2.2.1/elasticsearch-2.2.1.tar.gz"
	- tar -xvf elasticsearch-2.2.1.tar.gz

edit config file

    - cd elasticsearch-2.2.1/config/
	- nano elasticsearch.yml
		set cluster name => cluster.name: flink-kbe-cluster
		set (optional) data path => path.data: /.../es-data-2.2.1 (if you don't, it'll create dirs under elasticsearch-2.2.1)
		set (optional) log path => path.logs: /.../es-log-2.2.1
		save and exit

install useful plugins

    - cd elasticsearch-2.2.1/
	- bin/plugin install royrusso/elasticsearch-hq/v2.0.3
	- bin/plugin install lmenezes/elasticsearch-kopf

Elasticsearch can be used with some other plugins like Kibana and Sense for dev console (for running queries).

##### IT'S BETTER TO USE ES VIA KIBANA AND SENSE, FOLLOW THE INSTRUCTIONS! 
download kibana 4.4.0 (compatible with elasticsearch-2.2.1)

    - wget "https://download.elastic.co/kibana/kibana/kibana-4.4.0-linux-x64.tar.gz"
	- tar -xvf kibana-4.4.0-linux-x64.tar.gz
    - cd kibana-4.4.0-linux-x64/
	- bin/kibana plugin --install elastic/sense
    - bin/kibana
 
#### **Important!** Before starting the process, be sure you have the dump file in your local file system. You can download it [here](https://dumps.wikimedia.org/wikidatawiki/entities/latest-all.json.bz2). The input path and the other settings (e.g the number of threads) is configured from **config.properties**
#### STARTING PROCESSES

 - Prepare your package
	you should create a jar:
	 - cd /.../flink-kb-extractor 	
	 - mvn package
		 - jar will be available under /target/flink-kb-extractor-0.0.1-SNAPSHOT-jar-with-dependencies.jar
 - Start Elasticsearch
	- cd elasticsearch-2.2.1/
	- bin/elasticsearch -d (start as a daemon!)
 - (optional) check the logs to be sure that the ES instance is initialized properly.
	- Open your browser, go to http://localhost:9200/_plugin/hq/#cluster
	- Open another tab, go to http://localhost:9200/_plugin/kopf/#!/cluster 
 - Start Flink job manager 
	- cd flink-1.1.4
	- bin/start-local.sh (to stop:  bin/stop-local.sh) 
	Open another tab, go to http://localhost:8081 
 - Upload the package
	- Go to http://localhost:8081
	- Submit new Job/ Add new/ Upload your jar/
	- Click the checkbox, enter your entry class 
	- Press submit. 
	
  Congrats! You've started a new job!
 - (Optional) Start Kibana
	- cd kibana-4.4.0-linux-x64/
	- bin/kibana 
	- Go to http://localhost:5601/app/sense and play with your data :)





