package com.ofg.bankstatement.infrastruture.jaxb;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.StringReader;

public class JaxbUnmarshaller
{
    private JAXBContext jaxbContext;

    @SuppressWarnings("rawtypes")
    public JaxbUnmarshaller(Class... klasses)
    {
        try {
            this.jaxbContext = JAXBContext.newInstance(klasses);
        } catch (JAXBException e) {
            throw new RuntimeException("Error creating JAXB context!", e);
        }
    }

    public JaxbUnmarshaller(String contextPath)
    {
        try {
            this.jaxbContext = JAXBContext.newInstance(contextPath);
        } catch (JAXBException e) {
            throw new RuntimeException("Error creating JAXB context!", e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T unmarshall(String xml)
    {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Object object = unmarshaller.unmarshal(new StringReader(xml));
            if (object instanceof JAXBElement<?>) {
                return ((JAXBElement<T>) object).getValue();
            } else {
                return (T) object;
            }
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T unmarshall(InputStream inputStream)
    {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Object object = unmarshaller.unmarshal(inputStream);
            if (object instanceof JAXBElement<?>) {
                return ((JAXBElement<T>) object).getValue();
            } else {
                return (T) object;
            }
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
