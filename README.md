Real time sentimental analysis of tweets that have the hash tags #trump and #obama based on tweet locations to divide the tweets into Negative, Neutral and Positive sentiments.

The architectural components of this project is as follows:
1. Scrapper component (Producer) - Java program that uses the 'kafka-twitter-producer' dependency to stream current tweets that have the given hash tags and that are in the current location.
2. Transport Component - The scrapper program writes the data to a Kafka server with Zookeeper
3. Sentiment Analyzer (Consumer) - Java program that reads data using Spark streaming and then uses Stanford CoreNLP to determine the sentiment of the tweet. This program then writes the data to an elasticsearch document
4. Visualization component - Elasticsearch using Kibana to read the data and display the sentiment of different locations on a map of USA

Steps to run the project:
1. Import both the BigDataKafkaProducer and BigDataKafkaConsumer projects as Java Maven projects
2. Run the Producer program followed by the Consumer program
3. Once the consumer starts writing data to Elasticsearch, you can open Kibana UI (localhost:5601) and view the map visualizationsTw