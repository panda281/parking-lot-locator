package et.com.gebeya.authservice.config;

import et.com.gebeya.authservice.dto.request_dto.OtpRequest;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServer;
    public Map<String,Object> commonProps()
    {
        Map<String,Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServer);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }

    public Map<String, Object> stringProducerConfig()
    {
        Map<String,Object> props = commonProps();
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class);
        return props;
    }

    @Bean
    public ProducerFactory<String, String> stringProducerFactory()
    {
        return new DefaultKafkaProducerFactory<>(stringProducerConfig());
    }

    public Map<String,Object> dtoProducerConfig()
    {
        Map<String, Object> props = commonProps();
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return props;
    }

    @Bean
    public ProducerFactory<String, OtpRequest> messagedtoProducerFactory()
    {
        return new DefaultKafkaProducerFactory<>(dtoProducerConfig());
    }

    @Bean
    public KafkaTemplate<String,String> kafkaTemplate()
    {
        return new KafkaTemplate<>(stringProducerFactory());
    }
    @Bean
    public KafkaTemplate<String,OtpRequest> dtoKafkaTemplate()
    {
        return new KafkaTemplate<>(messagedtoProducerFactory());
    }


}

