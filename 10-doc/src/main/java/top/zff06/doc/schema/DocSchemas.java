package top.zff06.doc.schema;

import top.zff06.doc.common.Result;
import top.zff06.doc.model.Book;
import top.zff06.doc.vo.PageResult;

import java.util.List;

/**
 * 文档专用的"具名 schema"
 * <p>
 * Result 是泛型，方法签名上 springdoc 只看得见 Result，猜不出 T，UI 里就退化成笼统的 Result schema。
 * 做法：给常见的 Result&lt;X&gt; 组合各定义一个空壳子类，然后在 @ApiResponse 里
 * content = @Content(schema = @Schema(implementation = BookResult.class)) 指定，文档里就能展开到具体字段。
 * <p>
 * 这些类不参与接口序列化，只给注解当"类型占位"，所以不需要任何字段。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description DocSchemas
 **/
public final class DocSchemas {

    private DocSchemas() {
    }

    /**
     * Result&lt;Void&gt;：删除、演示异常这类无数据返回
     */
    public static class VoidResult extends Result<Void> {
        private VoidResult() {
            super(null, null, null);
        }
    }

    /**
     * Result&lt;Book&gt;：单本图书
     */
    public static class BookResult extends Result<Book> {
        private BookResult() {
            super(null, null, null);
        }
    }

    /**
     * Result&lt;PageResult&lt;Book&gt;&gt;：分页列表
     */
    public static class BookPageResult extends Result<PageResult<Book>> {
        private BookPageResult() {
            super(null, null, null);
        }
    }

    /**
     * Result&lt;List&lt;Book&gt;&gt;：不分页列表
     */
    public static class BookListResult extends Result<List<Book>> {
        private BookListResult() {
            super(null, null, null);
        }
    }
}
