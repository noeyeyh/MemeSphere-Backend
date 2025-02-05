package com.memesphere.domain.dashboard.service;

import com.memesphere.domain.collection.service.CollectionQueryService;
import com.memesphere.domain.dashboard.converter.DashboardConverter;
import com.memesphere.domain.dashboard.dto.response.DashboardOverviewResponse;
import com.memesphere.domain.chartdata.entity.ChartData;
import com.memesphere.domain.dashboard.dto.response.DashboardTrendListResponse;
import com.memesphere.domain.chartdata.repository.ChartDataRepository;
import com.memesphere.domain.user.entity.User;
import com.memesphere.domain.user.repository.UserRepository;
import com.memesphere.global.apipayload.code.status.ErrorStatus;
import com.memesphere.global.apipayload.exception.GeneralException;
import com.memesphere.domain.memecoin.entity.MemeCoin;
import com.memesphere.domain.memecoin.repository.MemeCoinRepository;
import com.memesphere.domain.search.converter.SearchConverter;
import com.memesphere.domain.search.dto.response.SearchPageResponse;
import com.memesphere.domain.search.entity.SortType;
import com.memesphere.domain.search.entity.ViewType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardQueryServiceImpl implements DashboardQueryService {
    private final UserRepository userRepository;
    private final MemeCoinRepository memeCoinRepository;
    private final ChartDataRepository chartDataRepository;
    private final CollectionQueryService collectionQueryService;

    // ** 총 거래량 및 총 개수 ** //
    @Override
    public DashboardOverviewResponse getOverview() {
        // 등록된 모든 밈코인의 총 거래량
        BigDecimal totalVolume = chartDataRepository.findTotalVolume();

        // 등록된 모든 밈코인 수
        Long totalCoin = memeCoinRepository.count();

        return DashboardConverter.toOverView(totalVolume, totalCoin);
    }

    // ** 트렌드 ** //
    @Override
    public DashboardTrendListResponse getTrendList() {
        // 거래량 top5 밈코인-차트데이터
        List<ChartData> dataList = chartDataRepository.findTop5OrderByVolumeDesc();

        // 코인 아이디 1 기준 기록 시간
        LocalDateTime recordedTime = chartDataRepository.findRecordedTimeByCoinId1();

        return DashboardConverter.toTrendList(recordedTime, dataList);
    }

    // ** 차트 ** //
    @Override
    public SearchPageResponse getChartPage(String email, ViewType viewType, SortType sortType, Integer pageNumber) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        List<Long> userCollectionIds = collectionQueryService.getUserCollectionIds(user.getId());

        int pageSize = switch (viewType) {
            case GRID -> 9;
            case LIST -> 20;
            default -> throw new GeneralException(ErrorStatus.UNSUPPORTED_VIEW_TYPE);
        };

        String sortField = switch (sortType) {
            case PRICE_CHANGE -> "c.priceChange";
            case VOLUME_24H -> "c.volume";
            case PRICE -> "c.price";
            default -> throw new GeneralException(ErrorStatus.UNSUPPORTED_SORT_TYPE);
        };

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, sortField));
        Page<MemeCoin> coinPage = memeCoinRepository.findAllLatestChartData(pageable);

        // null 체크 후 예외 처리}
        return SearchConverter.toSearchPageDTO(coinPage, viewType, userCollectionIds);
    }
}