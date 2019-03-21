package com.lanny.web.dispatcher;

import com.lanny.web.domain.BeanContainer;
import com.lanny.web.exception.NoSuchBeanException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Lanny Yao
 * @date 3/14/2019 11:30 AM
 */

@Component
public class MyBeanFactory {

    private List<BeanContainer> beanContainers;

    public MyBeanFactory() {
        this.beanContainers = new ArrayList<>();
    }


    public boolean isEmpty() {
        return beanContainers.isEmpty();
    }

    public List<BeanContainer> getBeans() {
        return beanContainers;
    }

    public Object getBean(String targetBean) {
        return beanContainers.stream()
                .filter(bean -> targetBean.equals(bean.getBeanName()))
                .map(BeanContainer::getInstance)
                .findAny()
                .orElseThrow(NoSuchBeanException::new);
    }

    public void register(String beanName, Object instance) {
        beanContainers.add(new BeanContainer(beanName, instance));
    }
}
