package com.lkd.bt.common.vo;

import com.lkd.bt.common.entity.Metadata;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Created by lkkings on 2023/9/4
 * 种子信息
 */


@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Data
public class MetadataVO {



    @JsonView(ListView.class)
    private Metadata metadata;

    /**
     * 长度的字符串形式, 例如 1.7G 387.3MB 16KB 等
     */
    @JsonView(ListView.class)
    private String length;

    /**
     * 文件信息对象 List
     */
    @JsonView(DetailView.class)
    private List<Info> infos;


    /**
     * 列表视图
     */
    public interface ListView{}

    /**
     * 详情视图
     */
    public interface  DetailView extends ListView{}

    public MetadataVO(Metadata metadata, String length) {
        this.metadata = metadata;
        this.length = length;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Info {
        /**
         * 名字
         * 如果是单文件为名字, 如果为多文件为路径, 如果为多文件多级路径,"/"分割,也就是文件名.
         */
        private String name;

        /**
         * 长度
         */
        private Long length;

        /**
         * 转换后长度
         */
        private String lengthStr;

        public Info(String name, Long length) {
            this.name = name;
            this.length = length;
        }
    }

}

