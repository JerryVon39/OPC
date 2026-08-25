package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * CMS 区块历史版本对象 cms_block_history（每区块最多 20 版）
 */
public class CmsBlockHistory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 历史ID */
    private Long historyId;

    /** 区块ID */
    private Long blockId;

    /** 版本号 */
    private Long version;

    private String title;

    private String subtitle;

    private String content;

    private String image;

    private String link;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public void setHistoryId(Long historyId) { this.historyId = historyId; }
    public Long getHistoryId() { return historyId; }

    public void setBlockId(Long blockId) { this.blockId = blockId; }
    public Long getBlockId() { return blockId; }

    public void setVersion(Long version) { this.version = version; }
    public Long getVersion() { return version; }

    public void setTitle(String title) { this.title = title; }
    public String getTitle() { return title; }

    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getSubtitle() { return subtitle; }

    public void setContent(String content) { this.content = content; }
    public String getContent() { return content; }

    public void setImage(String image) { this.image = image; }
    public String getImage() { return image; }

    public void setLink(String link) { this.link = link; }
    public String getLink() { return link; }

    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public String getUpdateBy() { return updateBy; }

    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public Date getUpdateTime() { return updateTime; }
}
