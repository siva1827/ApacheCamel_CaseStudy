package com.UST.fileExport.model;

import jakarta.xml.bind.annotation.XmlRegistry;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import javax.xml.namespace.QName;

@XmlRegistry
public class ObjectFactory {

    private static final QName _TrendXml_QNAME = new QName("", "TrendXml");
    private static final QName _ReviewXml_QNAME = new QName("", "ReviewXml");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link TrendXml }
     */
    public TrendXml createTrendXml() {
        return new TrendXml();
    }

    /**
     * Create an instance of {@link ReviewXml }
     */
    public ReviewXml createReviewXml() {
        return new ReviewXml();
    }

    /**
     * Create an instance of {@link ReviewXml.Review }
     */
    public ReviewXml.Review createReviewXmlReview() {
        return new ReviewXml.Review();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TrendXml }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "TrendXml")
    public JAXBElement<TrendXml> createTrendXmlElement(TrendXml value) {
        return new JAXBElement<>(_TrendXml_QNAME, TrendXml.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReviewXml }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "ReviewXml")
    public JAXBElement<ReviewXml> createReviewXmlElement(ReviewXml value) {
        return new JAXBElement<>(_ReviewXml_QNAME, ReviewXml.class, null, value);
    }
}