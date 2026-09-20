package com.example.buildpro.service;

import com.example.buildpro.entity.ServiceItem;

import java.util.List;
import java.util.Optional;

public interface ServiceItemService {
    List<ServiceItem> findAll();
    Optional<ServiceItem> findById(Long id);
    ServiceItem create(ServiceItem service);
    Optional<ServiceItem> update(Long id, ServiceItem update);
    boolean delete(Long id);
}
