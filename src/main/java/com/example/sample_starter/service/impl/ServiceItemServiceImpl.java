package com.example.sample_starter.service.impl;

import com.example.sample_starter.entity.ServiceItem;
import com.example.sample_starter.repository.ServiceItemRepository;
import com.example.sample_starter.service.ServiceItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ServiceItemServiceImpl implements ServiceItemService {

    private final ServiceItemRepository serviceItemRepository;

    @Override
    public List<ServiceItem> findAll() {
        return serviceItemRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Override
    public Optional<ServiceItem> findById(Long id) {
        return serviceItemRepository.findById(id);
    }

    @Override
    public ServiceItem create(ServiceItem service) {
        return serviceItemRepository.save(service);
    }

    @Override
    public Optional<ServiceItem> update(Long id, ServiceItem update) {
        return serviceItemRepository.findById(id).map(existing -> {
            existing.setTitle(update.getTitle());
            existing.setDescription(update.getDescription());
            existing.setDisplayOrder(update.getDisplayOrder());
            return serviceItemRepository.save(existing);
        });
    }

    @Override
    public boolean delete(Long id) {
        if (!serviceItemRepository.existsById(id)) {
            return false;
        }
        serviceItemRepository.deleteById(id);
        return true;
    }
}
