package top.zff06.doc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.zff06.doc.common.BusinessException;
import top.zff06.doc.common.ErrorCode;
import top.zff06.doc.dto.BookQuery;
import top.zff06.doc.dto.BookSaveRequest;
import top.zff06.doc.model.Book;
import top.zff06.doc.model.BookStatus;
import top.zff06.doc.vo.PageResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 图书服务（内存实现）
 * <p>
 * 模块 10 的重点是文档，存储用 ConcurrentHashMap 就够，不引数据库。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description BookService
 **/
@Slf4j
@Service
public class BookService {

    private final Map<Long, Book> store = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(0);

    public BookService() {
        initData();
    }

    private void initData() {
        save(seed("深入理解 Java 虚拟机", "周志明", "9787111641247", "129.00", 20, BookStatus.ON_SALE));
        save(seed("Effective Java", "Joshua Bloch", "9787111444076", "119.00", 8, BookStatus.ON_SALE));
        save(seed("Java 并发编程实战", "Brian Goetz", "9787111370048", "79.00", 0, BookStatus.SOLD_OUT));
    }

    private BookSaveRequest seed(String title, String author, String isbn,
                                 String price, Integer stock, BookStatus status) {
        BookSaveRequest request = new BookSaveRequest();
        request.setTitle(title);
        request.setAuthor(author);
        request.setIsbn(isbn);
        request.setPrice(new BigDecimal(price));
        request.setStock(stock);
        request.setStatus(status);
        return request;
    }

    public PageResult<Book> page(BookQuery query) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : Math.min(query.getPageSize(), 100);
        String keyword = query.getKeyword() == null ? "" : query.getKeyword().trim().toLowerCase(Locale.ROOT);

        List<Book> matched = new ArrayList<>(store.values()).stream()
                .filter(b -> keyword.isEmpty()
                        || b.getTitle().toLowerCase(Locale.ROOT).contains(keyword)
                        || b.getAuthor().toLowerCase(Locale.ROOT).contains(keyword))
                .filter(b -> query.getStatus() == null || query.getStatus() == b.getStatus())
                .sorted(Comparator.comparing(Book::getId))
                .toList();

        int from = Math.min((pageNum - 1) * pageSize, matched.size());
        int to = Math.min(from + pageSize, matched.size());
        return new PageResult<>(new ArrayList<>(matched.subList(from, to)), matched.size(), pageNum, pageSize);
    }

    public Book get(Long id) {
        Book book = store.get(id);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
        return book;
    }

    public Book create(BookSaveRequest request) {
        checkIsbnUnique(request.getIsbn(), null);
        return save(request);
    }

    public Book update(Long id, BookSaveRequest request) {
        Book exist = get(id);
        checkIsbnUnique(request.getIsbn(), id);
        Book book = new Book(id, request.getTitle(), request.getAuthor(), request.getIsbn(),
                request.getPrice().doubleValue(), request.getStock(),
                request.getStatus() == null ? BookStatus.ON_SALE : request.getStatus());
        store.put(id, book);
        log.info("更新图书：id={}，原书名={}", id, exist.getTitle());
        return book;
    }

    public void delete(Long id) {
        get(id);
        store.remove(id);
        log.info("删除图书：id={}", id);
    }

    /**
     * 演示 500 的接口：@Operation(deprecated = true) 标记在 Controller 上（练习 4）
     */
    public void boom() {
        throw new IllegalStateException("演示 500：服务端未预料到的异常");
    }

    private Book save(BookSaveRequest request) {
        long id = idGen.incrementAndGet();
        Book book = new Book(id, request.getTitle(), request.getAuthor(), request.getIsbn(),
                request.getPrice().doubleValue(), request.getStock(),
                request.getStatus() == null ? BookStatus.ON_SALE : request.getStatus());
        store.put(id, book);
        return book;
    }

    private void checkIsbnUnique(String isbn, Long selfId) {
        boolean duplicated = store.values().stream()
                .anyMatch(b -> b.getIsbn().equals(isbn) && !b.getId().equals(selfId));
        if (duplicated) {
            throw new BusinessException(ErrorCode.ISBN_DUPLICATED);
        }
    }
}
