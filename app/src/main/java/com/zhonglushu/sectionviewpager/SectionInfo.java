package com.zhonglushu.sectionviewpager;

public class SectionInfo {

    static final int NO_ID = -1;

    /**
     * The id in the settings database for this item
     */
    public long id = NO_ID;

    public String packageName;
    public String className;
    public String title;
    public String contentDescription;
    public int sectionType;
    public String layoutName;
    public int screenId;
    public int cellX;
    public int cellY;
    public int spanX;
    public int spanY;

    SectionInfo() {
        spanX = 1;
        spanY = 1;
    }

    SectionInfo(SectionInfo info) {
        copyFrom(info);
    }

    public SectionInfo(Long id) {
        this.id = id;
    }

    public SectionInfo(Long id, String packageName, String className,
                       String title, String contentDescription,
                       Integer sectionType, String layoutName,
                       Integer screenId, Integer cellX,
                       Integer cellY, Integer spanX, Integer spanY) {
        this.id = id;
        this.packageName = packageName;
        this.className = className;
        this.title = title;
        this.contentDescription = contentDescription;
        this.sectionType = sectionType;
        this.layoutName = layoutName;
        this.screenId = screenId;
        this.cellX = cellX;
        this.cellY = cellY;
        this.spanX = spanX;
        this.spanY = spanY;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Not-null value.
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Not-null value; ensure this value is available before it is saved to the
     * database.
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentDescription() {
        return contentDescription;
    }

    public void setContentDescription(String contentDescription) {
        this.contentDescription = contentDescription;
    }

    public Integer getSectionType() {
        return sectionType;
    }

    public void setSectionType(Integer sectionType) {
        this.sectionType = sectionType;
    }

    public Integer getScreenId() {
        return screenId;
    }

    public void setScreenId(Integer screenId) {
        this.screenId = screenId;
    }

    public Integer getCellX() {
        return cellX;
    }

    public void setCellX(Integer cellX) {
        this.cellX = cellX;
    }

    public Integer getCellY() {
        return cellY;
    }

    public void setCellY(Integer cellY) {
        this.cellY = cellY;
    }

    public Integer getSpanX() {
        return spanX;
    }

    public void setSpanX(Integer spanX) {
        this.spanX = spanX;
    }

    public Integer getSpanY() {
        return spanY;
    }

    public void setSpanY(Integer spanY) {
        this.spanY = spanY;
    }

    public String getLayoutName() {
        return layoutName;
    }

    public void setLayoutName(String layoutName) {
        this.layoutName = layoutName;
    }

    public void copyFrom(SectionInfo info) {
        this.id = info.id;
        this.packageName = info.packageName;
        this.className = info.className;
        this.title = info.title;
        this.contentDescription = info.contentDescription;
        this.sectionType = info.sectionType;
        this.layoutName = info.layoutName;
        this.screenId = info.screenId;
        this.cellX = info.cellX;
        this.cellY = info.cellY;
        this.spanX = info.spanX;
        this.spanY = info.spanY;
    }
}
