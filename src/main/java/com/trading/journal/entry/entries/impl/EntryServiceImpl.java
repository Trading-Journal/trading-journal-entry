package com.trading.journal.entry.entries.impl;

import com.allanweber.jwttoken.data.AccessTokenInfo;
import com.trading.journal.entry.ApplicationException;
import com.trading.journal.entry.balance.BalanceService;
import com.trading.journal.entry.entries.CalculateEntry;
import com.trading.journal.entry.entries.Entry;
import com.trading.journal.entry.entries.EntryRepository;
import com.trading.journal.entry.entries.EntryService;
import com.trading.journal.entry.journal.Journal;
import com.trading.journal.entry.journal.JournalService;
import com.trading.journal.entry.queries.CollectionName;
import com.trading.journal.entry.queries.data.PageResponse;
import com.trading.journal.entry.queries.data.PageableRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiFunction;

@RequiredArgsConstructor
@Service
public class EntryServiceImpl implements EntryService {

    private final EntryRepository repository;

    private final JournalService journalService;

    private final BalanceService balanceService;

    @Override
    public PageResponse<Entry> query(AccessTokenInfo accessToken, String journalId, PageableRequest pageRequest) {
        CollectionName collectionName = collectionName().apply(accessToken, journalId);
        Page<Entry> page = repository.findAll(collectionName, pageRequest);
        return new PageResponse<>(page);
    }

    @Override
    public List<Entry> getAll(AccessTokenInfo accessToken, String journalId) {
        CollectionName collectionName = collectionName().apply(accessToken, journalId);
        PageableRequest pageableRequest = PageableRequest.builder()
                .page(0)
                .size(Integer.MAX_VALUE)
                .sort(Sort.by("date").ascending())
                .build();
        Page<Entry> all = repository.findAll(collectionName, pageableRequest);
        return all.stream().toList();
    }

    @Override
    public Entry save(AccessTokenInfo accessToken, String journalId, Entry entry) {
        BigDecimal balance = balanceService.getCurrentBalance(accessToken, journalId, entry.getDate());

        CalculateEntry calculateEntry = new CalculateEntry(entry, balance);
        Entry calculated = calculateEntry.calculate();

        CollectionName collectionName = collectionName().apply(accessToken, journalId);
        return repository.save(collectionName, calculated);
    }

    @Override
    public void delete(AccessTokenInfo accessToken, String journalId, String entryId) {
        CollectionName collectionName = collectionName().apply(accessToken, journalId);
        Entry entry = get(collectionName, entryId);
        repository.delete(collectionName, entry);
    }

    private Entry get(CollectionName collectionName, String entryId) {
        return repository.getById(collectionName, entryId)
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "Entry not found"));
    }

    private BiFunction<AccessTokenInfo, String, CollectionName> collectionName() {
        return (accessTokenInfo, journalId) -> {
            Journal journal = journalService.get(accessTokenInfo, journalId);
            return new CollectionName(accessTokenInfo, journal.getName());
        };
    }
}
