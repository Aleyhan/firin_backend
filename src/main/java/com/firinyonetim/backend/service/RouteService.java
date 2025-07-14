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
import com.firinyonetim.backend.dto.route.request.RouteUpdateRequest; // YENİ IMPORT
import com.firinyonetim.backend.exception.ResourceNotFoundException; // YENİ IMPORT


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final RouteAssignmentRepository routeAssignmentRepository;
    private final CustomerRepository customerRepository;
    private final RouteMapper routeMapper;
    private final CustomerMapper customerMapper;


    @Transactional // createRoute metodunun @Transactional olduğundan emin olun
    public RouteResponse createRoute(RouteCreateRequest request) {
        // 1. Benzersizlik kontrolü
        if (routeRepository.existsByRouteCode(request.getRouteCode())) {
            throw new IllegalStateException("Rota kodu '" + request.getRouteCode() + "' zaten kullanılıyor.");
        }

        // 2. Mapper ile entity'e çevir
        Route route = routeMapper.toRoute(request);

        // 3. Kaydet ve response'a çevirip döndür
        return routeMapper.toRouteResponse(routeRepository.save(route));
    }

    public List<RouteResponse> getAllRoutes() {
        return routeRepository.findAll().stream()
                .map(routeMapper::toRouteResponse)
                .collect(Collectors.toList());
    }

    // 2. Rota silme metodunu GÜNCELLE (Artık pasif hale getirecek)
    @Transactional
    public void deleteRouteByStatus(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));

        // Gerçek silme yerine, rotayı pasif hale getiriyoruz.
        route.setActive(false);
        routeRepository.save(route);
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
    public RouteResponse updateRoute(Long routeId, RouteUpdateRequest request) { // <<< DÜZELTİLDİ
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));

        // 'request' artık doğru tipte (RouteUpdateRequest) olduğu için mapper sorunsuz çalışacak.
        routeMapper.updateRouteFromDto(request, route);

        Route savedRoute = routeRepository.save(route);
        return routeMapper.toRouteResponse(savedRoute);
    }

    public RouteResponse getRouteById(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));
        return routeMapper.toRouteResponse(route);
    }

    @Transactional
    public void updateRouteCustomers(Long routeId, List<Long> customerIds) {
        // 1. Rotanın var olup olmadığını kontrol et
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));

        // 2. Bu rotanın mevcut tüm müşteri atamalarını sil
        // Bunun için RouteAssignmentRepository'ye yeni bir metot ekleyeceğiz.
        routeAssignmentRepository.deleteByRouteId(routeId);

        // 3. Gelen listedeki yeni müşterileri rotaya ata
        if (customerIds != null && !customerIds.isEmpty()) {
            List<Customer> customers = customerRepository.findAllById(customerIds);
            // Gelen ID'lerin hepsi geçerli mi diye kontrol edilebilir, şimdilik basit tutalım.

            customers.forEach(customer -> {
                RouteAssignment assignment = new RouteAssignment();
                assignment.setRoute(route);
                assignment.setCustomer(customer);
                routeAssignmentRepository.save(assignment);
            });
        }
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

    public Map<Long, Long> getCustomerCountsPerRoute() { // <<< İMZA DEĞİŞTİ
        List<Route> routes = routeRepository.findAllWithCustomers();
        return routes.stream()
                .collect(Collectors.toMap(
                        Route::getId, // <<< DEĞİŞİKLİK BURADA: Rota ID'sini anahtar yapıyor
                        route -> (long) route.getAssignments().size()
                ));
    }

    // ... diğer metotlar

    @Transactional
        public RouteResponse toggleRouteStatus(Long routeId) {
            Route route = routeRepository.findById(routeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));
            route.setActive(!route.isActive()); // Mevcut durumu tersine çevir
            Route updatedRoute = routeRepository.save(route);
            return routeMapper.toRouteResponse(updatedRoute); // Mevcut mapper'ı kullanarak dönüşüm yap
        }
}