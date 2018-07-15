package com.utdallas.hpt150030;

import com.google.gson.Gson;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;

/**
 * @author Hardik
 */
public class BigDataKafkaProducer {

    public static GeoLocation locations[] = {
            new GeoLocation(40.7128, -74.0059),
            new GeoLocation(32.776664, -96.796988),
            new GeoLocation(34.052234, -118.243685),
            new GeoLocation(41.878114, -87.629798),
            new GeoLocation(30.267153, -97.743061),
            new GeoLocation(37.338208, -121.886329),
            new GeoLocation(39.952584, -75.165222),
            new GeoLocation(42.360082, -71.058880),
            new GeoLocation(33.748995, -84.387982),
            new GeoLocation(39.103118, -84.512020),
            new GeoLocation(30.332184, -81.655651),
            new GeoLocation(35.227087, -80.843127),
            new GeoLocation(47.606209, -122.332071),
            new GeoLocation(42.331427, -83.045754),
            new GeoLocation(36.169941, -115.139830),
            new GeoLocation(39.739236, -104.990251)
    };

    public static final Random random = new Random();

    public static final Gson gson = new Gson();

    public static void main(final String[] args) {
        if (args.length < 1) {
            System.out.println("Please give hash tags as command line arguments");
            return;
        }
        //Configure the Producer
        Properties configProperties = new Properties();
        configProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        configProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        final Producer producer = new KafkaProducer<String, String>(configProperties);
        System.out.println("Starting Kafka Producer ====================================================");
        final String topicName = "bigdata";
        File file = new File("twitter4j.properties");
        Properties prop = new Properties();
        InputStream is = null;
        try {
            if (file.exists()) {
                is = new FileInputStream(file);
                prop.load(is);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(-1);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignore) {
                }
            }
        }
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.setOAuthConsumerKey(prop.getProperty("oauth.consumerKey"))
                .setOAuthConsumerSecret(prop.getProperty("oauth.consumerSecret"))
                .setOAuthAccessToken(prop.getProperty("oauth.accessToken"))
                .setOAuthAccessTokenSecret(prop.getProperty("oauth.accessTokenSecret"));
        TwitterStream twitterStream = new TwitterStreamFactory(configurationBuilder.build()).getInstance();
        StatusListener listener = new StatusListener() {
            public void onStatus(Status status) {
                System.out.print("Sending === > ");
                System.out.println("@" + status.getUser().getScreenName() + " - " + status.getText());
                if (status.getGeoLocation() != null) {
                    System.out.println("GeoLocation Found ==== " + status.getGeoLocation());
                }
                TwitterAnalysisBean bean = new TwitterAnalysisBean();

                List<String> queryStrings = Arrays.asList(args[0].split(","));
                for (String hash : queryStrings) {
                    if (status.getText().toLowerCase().contains(hash.toLowerCase())) {
                        bean.setTag(hash);
                    }
                }

                GeoLocation location = status.getGeoLocation() != null ? status.getGeoLocation() : locations[random.nextInt(locations.length)];
                bean.setLat(location.getLatitude());
                bean.setLon(location.getLongitude());
                bean.setTweet(status.getText());
                ProducerRecord<String, String> rec = new ProducerRecord<String, String>(topicName, gson.toJson(bean));
                producer.send(rec);
            }

            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
                //System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
            }

            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                //System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
            }

            public void onScrubGeo(long userId, long upToStatusId) {
                //System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
            }

            public void onStallWarning(StallWarning warning) {
                //System.out.println("Got stall warning:" + warning);
            }

            public void onException(Exception ex) {
                ex.printStackTrace();
            }
        };
        twitterStream.addListener(listener);
        twitterStream.sample();
        FilterQuery tweetQueryFilter = new FilterQuery();
        tweetQueryFilter.track(args[0].split(","));
        twitterStream.filter(tweetQueryFilter);
    }
}
