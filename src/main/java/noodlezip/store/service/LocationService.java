package noodlezip.store.service;

import lombok.RequiredArgsConstructor;
import noodlezip.store.dto.LocationInfoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LocationService {

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public LocationInfoDto getLocationInfo(String roadAddress) {
        // 1. 주소 → 좌표 변환
        String url = "https://dapi.kakao.com/v2/local/search/address.json?query=" +
                UriUtils.encode(roadAddress, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        List<?> docs = (List<?>) res.getBody().get("documents");

        if (docs == null || docs.isEmpty()) {
            throw new IllegalArgumentException("주소로 좌표를 찾을 수 없습니다.");
        }

        Map firstDoc = (Map) docs.get(0);
        String x = (String) firstDoc.get("x"); // 경도
        String y = (String) firstDoc.get("y"); // 위도

        Double lat;
        Double lng;
        try {
            lat = Double.parseDouble(y);
            lng = Double.parseDouble(x);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("위도 또는 경도 변환 실패", e);
        }

        // 2. 좌표 → 법정동 코드 조회
        String coordUrl = "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json?x=" + x + "&y=" + y;
        ResponseEntity<Map> coordRes = restTemplate.exchange(coordUrl, HttpMethod.GET, entity, Map.class);
        List<?> coordDocs = (List<?>) coordRes.getBody().get("documents");

        String legalCode = coordDocs.stream()
                .map(o -> (Map<?, ?>) o)
                .filter(doc -> "B".equals(doc.get("region_type")))
                .map(doc -> (String) doc.get("code"))
                .findFirst()
                .orElse(null);

        Long legalCodeInt = null;
        if (legalCode != null) {
            try {
                legalCodeInt = Long.parseLong(legalCode);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("법정동 코드 변환 실패", e);
            }
        }

        return new LocationInfoDto(lat, lng, legalCodeInt);
    }
}