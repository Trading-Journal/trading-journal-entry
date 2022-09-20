package com.trading.journal.entry.balance.impl;

import com.allanweber.jwttoken.data.AccessTokenInfo;
import com.trading.journal.entry.entries.Entry;
import com.trading.journal.entry.entries.EntryRepository;
import com.trading.journal.entry.journal.Journal;
import com.trading.journal.entry.journal.JournalService;
import com.trading.journal.entry.queries.CollectionName;
import com.trading.journal.entry.queries.QueryCriteriaBuilder;
import com.trading.journal.entry.queries.data.Filter;
import com.trading.journal.entry.queries.data.FilterOperation;
import com.trading.journal.entry.queries.data.PageableRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class BalanceServiceImplTest {

    private static AccessTokenInfo accessToken;

    private static CollectionName collectionName;

    @Mock
    EntryRepository entryRepository;

    @Mock
    JournalService journalService;

    @InjectMocks
    BalanceServiceImpl balanceService;

    @BeforeAll
    static void setUp() {
        accessToken = new AccessTokenInfo("subject", 1L, "TENANCY", emptyList());
        collectionName = new CollectionName(accessToken, "journal");
    }

    @DisplayName("Start balance positive and many entries with different net results return a positive balance")
    @Test
    void balancePositive() {
        LocalDateTime date = LocalDateTime.now();
        String journalId = "123456";
        BigDecimal startBalance = BigDecimal.valueOf(100);

        when(journalService.get(accessToken, journalId)).thenReturn(Journal.builder().name("journal").startBalance(startBalance).build());

        PageableRequest pageableRequest = PageableRequest.builder()
                .page(0)
                .size(Integer.MAX_VALUE)
                .sort(Sort.by("date").ascending())
                .filters(singletonList(Filter.builder().field("date").operation(FilterOperation.LESS_THAN_OR_EQUAL_TO).value(date.format(QueryCriteriaBuilder.DATE_FORMATTER)).build()))
                .build();

        List<Entry> entries = asList(
                Entry.builder().netResult(BigDecimal.valueOf(50.31)).build(),
                Entry.builder().netResult(BigDecimal.valueOf(-35.59)).build(),
                Entry.builder().netResult(BigDecimal.valueOf(71.23)).build(),
                Entry.builder().netResult(BigDecimal.valueOf(-12.67)).build(),
                Entry.builder().netResult(BigDecimal.valueOf(22.67)).build(),
                Entry.builder().netResult(BigDecimal.valueOf(-55.99)).build()
        );

        Page<Entry> page = new PageImpl<>(entries, pageableRequest.pageable(), 1L);
        when(entryRepository.findAll(collectionName, pageableRequest)).thenReturn(page);

        BigDecimal currentBalance = balanceService.getCurrentBalance(accessToken, journalId, date);

        assertThat(currentBalance).isEqualTo(BigDecimal.valueOf(139.96));
    }

    @DisplayName("Start balance positive and many entries with different net results return a negative balance")
    @Test
    void balanceNegative() {
        LocalDateTime date = LocalDateTime.now();
        String journalId = "123456";
        BigDecimal startBalance = BigDecimal.valueOf(100);

        when(journalService.get(accessToken, journalId)).thenReturn(Journal.builder().name("journal").startBalance(startBalance).build());

        PageableRequest pageableRequest = PageableRequest.builder()
                .page(0)
                .size(Integer.MAX_VALUE)
                .sort(Sort.by("date").ascending())
                .filters(singletonList(Filter.builder().field("date").operation(FilterOperation.LESS_THAN_OR_EQUAL_TO).value(date.format(QueryCriteriaBuilder.DATE_FORMATTER)).build()))
                .build();

        List<Entry> entries = asList(
                Entry.builder().netResult(BigDecimal.valueOf(50.31)).build(),
                Entry.builder().netResult(BigDecimal.valueOf(-35.59)).build(),
                Entry.builder().netResult(BigDecimal.valueOf(-71.23)).build(),
                Entry.builder().netResult(BigDecimal.valueOf(-12.67)).build(),
                Entry.builder().netResult(BigDecimal.valueOf(-22.67)).build(),
                Entry.builder().netResult(BigDecimal.valueOf(-52.29)).build()
        );

        Page<Entry> page = new PageImpl<>(entries, pageableRequest.pageable(), 1L);
        when(entryRepository.findAll(collectionName, pageableRequest)).thenReturn(page);

        BigDecimal currentBalance = balanceService.getCurrentBalance(accessToken, journalId, date);

        assertThat(currentBalance).isEqualTo(BigDecimal.valueOf(-44.14));
    }

    @DisplayName("Start balance negative and many entries with different net results return a positive balance")
    @Test
    void balancePositiveWithStartBalanceNegative() {
        LocalDateTime date = LocalDateTime.now();
        String journalId = "123456";
        BigDecimal startBalance = BigDecimal.valueOf(-100);

        when(journalService.get(accessToken, journalId)).thenReturn(Journal.builder().name("journal").startBalance(startBalance).build());

        PageableRequest pageableRequest = PageableRequest.builder()
                .page(0)
                .size(Integer.MAX_VALUE)
                .sort(Sort.by("date").ascending())
                .filters(singletonList(Filter.builder().field("date").operation(FilterOperation.LESS_THAN_OR_EQUAL_TO).value(date.format(QueryCriteriaBuilder.DATE_FORMATTER)).build()))
                .build();

        List<Entry> entries = asList(
                Entry.builder().netResult(BigDecimal.valueOf(50.31)).build(),
                Entry.builder().netResult(BigDecimal.valueOf(35.59)).build(),
                Entry.builder().netResult(BigDecimal.valueOf(71.23)).build(),
                Entry.builder().netResult(BigDecimal.valueOf(-12.67)).build(),
                Entry.builder().netResult(BigDecimal.valueOf(22.67)).build(),
                Entry.builder().netResult(BigDecimal.valueOf(33.88)).build(),
                Entry.builder().netResult(BigDecimal.valueOf(-55.99)).build(),
                Entry.builder().netResult(BigDecimal.valueOf(-45.01)).build()
        );

        Page<Entry> page = new PageImpl<>(entries, pageableRequest.pageable(), 1L);
        when(entryRepository.findAll(collectionName, pageableRequest)).thenReturn(page);

        BigDecimal currentBalance = balanceService.getCurrentBalance(accessToken, journalId, date);

        assertThat(currentBalance).isEqualTo(BigDecimal.valueOf(0.01));
    }

}