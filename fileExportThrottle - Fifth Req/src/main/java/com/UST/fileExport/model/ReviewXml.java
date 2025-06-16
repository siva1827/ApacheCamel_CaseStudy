package com.UST.fileExport.model;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "ReviewXml")
public class ReviewXml {
    private String itemId;
    private List<Review> reviews;

    public static class Review {
        private int reviewrating;
        private String reviewcomment;

        @XmlElement(name = "reviewrating")
        public int getReviewrating() { return reviewrating; }
        public void setReviewrating(int reviewrating) { this.reviewrating = reviewrating; }

        @XmlElement(name = "reviewcomment")
        public String getReviewcomment() { return reviewcomment; }
        public void setReviewcomment(String reviewcomment) { this.reviewcomment = reviewcomment; }
    }

    @XmlElement
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    @XmlElementWrapper(name = "reviews")
    @XmlElement(name = "review")
    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }
}