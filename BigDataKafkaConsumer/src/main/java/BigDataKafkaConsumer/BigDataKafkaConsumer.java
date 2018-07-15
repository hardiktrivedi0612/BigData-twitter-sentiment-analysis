package BigDataKafkaConsumer;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;
import org.elasticsearch.hadoop.cfg.ConfigurationOptions;
import org.elasticsearch.spark.streaming.api.java.JavaEsSparkStreaming;
import scala.Tuple2;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Hardik
 */
public class BigDataKafkaConsumer {

    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Kafka Consumer ===========================================================");
        BigDataKafkaConsumer consumer = new BigDataKafkaConsumer();
        consumer.consume();
    }

    private void consume() {
        SparkConf sparkConf = new SparkConf().setMaster("local[4]").setAppName("BigDataKafkaConsumer");
        //ElasticSearch Conf
        sparkConf.set("es.index.auto.create", "true");
        JavaStreamingContext streamingContext = new JavaStreamingContext(sparkConf, new Duration(2000));

        int numOfThreads = 4;
        Map<String, Integer> topicMap = new HashMap<String, Integer>();
        topicMap.put("bigdata", numOfThreads);

        JavaPairReceiverInputDStream<String, String> messages =
                KafkaUtils.createStream(streamingContext, "localhost", "group1", topicMap);

        //Upload to elastic search code
        JavaDStream<String> lines = messages.map(Tuple2::_2);
        JavaDStream<TwitterAnalysisBean> beans = lines.map(x -> TwitterAnalysisBean.applySentimentalAnalysis(
                gson.fromJson(x, TwitterAnalysisBean.class)));
        JavaEsSparkStreaming.saveToEs(beans, "bigdata/docs",
                ImmutableMap.of(ConfigurationOptions.ES_MAPPING_TIMESTAMP, "timeStamp"));
        beans.print();

        streamingContext.start();
        streamingContext.awaitTermination();
    }
}
