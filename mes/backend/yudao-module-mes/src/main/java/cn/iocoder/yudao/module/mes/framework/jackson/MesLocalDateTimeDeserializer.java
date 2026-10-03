package cn.iocoder.yudao.module.mes.framework.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class MesLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int MIN_VALID_YEAR = 2000;
    private static final List<DateTimeFormatter> LOCAL_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        if (token == JsonToken.VALUE_NUMBER_INT) {
            return fromEpoch(parser.getLongValue());
        }
        if (token == JsonToken.VALUE_STRING) {
            return parseText(parser.getText());
        }
        return null;
    }

    private LocalDateTime parseText(String rawValue) {
        String text = rawValue == null ? "" : rawValue.trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.matches("-?\\d+")) {
            try {
                return fromEpoch(Long.parseLong(text));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        for (DateTimeFormatter formatter : LOCAL_FORMATTERS) {
            try {
                return valid(LocalDateTime.parse(text, formatter));
            } catch (DateTimeParseException ignored) {
                // Try the next supported format.
            }
        }
        try {
            return valid(OffsetDateTime.parse(text).atZoneSameInstant(DEFAULT_ZONE).toLocalDateTime());
        } catch (DateTimeParseException ignored) {
            // Try Instant next.
        }
        try {
            return valid(Instant.parse(text).atZone(DEFAULT_ZONE).toLocalDateTime());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalDateTime fromEpoch(long epochValue) {
        long epochMillis = Math.abs(epochValue) < 100_000_000_000L ? epochValue * 1000 : epochValue;
        return valid(Instant.ofEpochMilli(epochMillis).atZone(DEFAULT_ZONE).toLocalDateTime());
    }

    private LocalDateTime valid(LocalDateTime value) {
        return value != null && value.getYear() >= MIN_VALID_YEAR ? value : null;
    }
}
