package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.customer.response.CustomerResponse;
import com.firinyonetim.backend.dto.route.request.RouteCreateRequest;
import com.firinyonetim.backend.dto.route.response.RouteResponse;
import com.firinyonetim.backend.entity.Customer;
import com.firinyonetim.backend.entity.Route;
import com.firinyonetim.backend.entity.RouteAssignment;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.mapper.CustomerMapper;
import com.firinyonetim.backend.mapper.RouteMapper;
import com.firinyonetim.backend.repository.CustomerRepository;
import com.firinyonetim.backend.repository.RouteAssignmentRepository;
import com.firinyonetim.backend.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final RouteAssignmentRepository routeAssignmentRepository;
    private final CustomerRepository customerRepository;
    private final RouteMapper routeMapper;
    private final CustomerMapper customerMapper;

    // --- Rota (Liste) CRUD İşlemleri ---
    public RouteResponse createRoute(RouteCreateRequest request) {
        Route route = routeMapper.toRoute(request);
        return routeMapper.toRouteResponse(routeRepository.save(route));
    }

    public List<RouteResponse> getAllRoutes() {
        return routeRepository.findAll().stream()
                .map(routeMapper::toRouteResponse)
                .collect(Collectors.toList());
    }

    public void deleteRoute(Long routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new ResourceNotFoundException("Route not found with id: " + routeId);
        }
        routeRepository.deleteById(routeId);
    }

    // --- Atama İşlemleri ---
    @Transactional
    public void assignCustomerToRoute(Long routeId, Long customerId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        RouteAssignment assignment = new RouteAssignment();
        assignment.setRoute(route);
        assignment.setCustomer(customer);
        routeAssignmentRepository.save(assignment);
    }

    @Transactional
    public void removeCustomerFromRoute(Long routeId, Long customerId) {
        // Bu metot daha verimli hale getirilebilir, şimdilik basit tutuyoruz.
        routeAssignmentRepository.findByRouteId(routeId).stream()
                .filter(assignment -> assignment.getCustomer().getId().equals(customerId))
                .findFirst()
                .ifPresent(routeAssignmentRepository::delete);
    }

    public List<CustomerResponse> getCustomersByRoute(Long routeId) {
        return routeAssignmentRepository.findByRouteId(routeId).stream()
                .map(RouteAssignment::getCustomer)
                .map(customerMapper::toCustomerResponse)
                .collect(Collectors.toList());
    }

    public List<RouteResponse> getRoutesByCustomer(Long customerId) {
        return routeAssignmentRepository.findByCustomerId(customerId).stream()
                .map(RouteAssignment::getRoute)
                .map(routeMapper::toRouteResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateCustomerRoutes(Long customerId, List<Long> routeIds) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        // Müşterinin mevcut tüm atamalarını sil
        routeAssignmentRepository.deleteByCustomerId(customerId);

        // Yeni atamaları ekle
        if (routeIds != null && !routeIds.isEmpty()) {
            List<Route> routes = routeRepository.findAllById(routeIds);
            routes.forEach(route -> {
                RouteAssignment assignment = new RouteAssignment();
                assignment.setCustomer(customer);
                assignment.setRoute(route);
                routeAssignmentRepository.save(assignment);
            });
        }
    }
}