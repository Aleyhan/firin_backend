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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public void assignCustomersToRoute(Long routeId, List<Long> customerIds) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));

        // Get all current assignments for the route
        List<RouteAssignment> existingAssignments = routeAssignmentRepository.findByRouteId(routeId);
        Set<Long> existingCustomerIds = existingAssignments.stream()
                .map(assignment -> assignment.getCustomer().getId())
                .collect(Collectors.toSet());

        Set<Long> desiredCustomerIds = new java.util.HashSet<>(customerIds);

        // --- Step 1: Identify and remove customers no longer on the route ---
        List<RouteAssignment> assignmentsToRemove = existingAssignments.stream()
                .filter(assignment -> !desiredCustomerIds.contains(assignment.getCustomer().getId()))
                .collect(Collectors.toList());

        if (!assignmentsToRemove.isEmpty()) {
            routeAssignmentRepository.deleteAll(assignmentsToRemove);
        }

        // --- Step 2: Identify and add new customers ---
        List<Long> newCustomerIds = desiredCustomerIds.stream()
                .filter(id -> !existingCustomerIds.contains(id))
                .collect(Collectors.toList());

        if (!newCustomerIds.isEmpty()) {
            List<Customer> customersToAdd = customerRepository.findAllById(newCustomerIds);
            List<RouteAssignment> newAssignments = customersToAdd.stream().map(customer -> {
                RouteAssignment assignment = new RouteAssignment();
                assignment.setRoute(route);
                assignment.setCustomer(customer);
                return assignment;
            }).collect(Collectors.toList());

            routeAssignmentRepository.saveAll(newAssignments);
        }
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

        // 2. Bu rotaya zaten atanmış olan müşteri ID'lerinin bir set'ini oluştur.
        // Bu, veritabanına tekrar tekrar "bu atama var mı?" diye sormamızı engeller.
        List<Long> existingCustomerIds = routeAssignmentRepository.findByRouteId(routeId).stream()
                .map(assignment -> assignment.getCustomer().getId())
                .collect(Collectors.toList());

        // 3. Gelen listeden sadece yeni (henüz atanmamış) müşteri ID'lerini filtrele.
        List<Long> newCustomerIds = customerIds.stream()
                .filter(id -> !existingCustomerIds.contains(id))
                .collect(Collectors.toList());

        List<Long> mergedCustomerIds = Stream.concat(existingCustomerIds.stream(), newCustomerIds.stream())
                .distinct() // Remove duplicates if any
                .collect(Collectors.toList());

        // 4. Yeni müşteriler varsa, onları bul ve rotaya ata.
        if (!mergedCustomerIds.isEmpty()) {
            List<Customer> customersToAdd = customerRepository.findAllById(mergedCustomerIds);

            List<RouteAssignment> newAssignments = customersToAdd.stream().map(customer -> {
                RouteAssignment assignment = new RouteAssignment();
                assignment.setRoute(route);
                assignment.setCustomer(customer);
                return assignment;
            }).collect(Collectors.toList());

            // Performans için toplu kaydetme
            routeAssignmentRepository.saveAll(newAssignments);
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

    // YENİ METOT
    @Transactional
    public void addCustomersToRoute(Long routeId, List<Long> customerIds) {
        // 1. Rotayı ve ilişkili atamalarını getir.
        // findById kullanmak burada daha verimli olabilir, çünkü tüm müşteri nesnelerine ihtiyacımız yok.
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));

        // 2. Bu rotaya zaten atanmış olan müşteri ID'lerinin bir set'ini oluştur.
        // Bu, veritabanına tekrar tekrar "bu atama var mı?" diye sormamızı engeller.
        Set<Long> existingCustomerIds = route.getAssignments().stream()
                .map(assignment -> assignment.getCustomer().getId())
                .collect(Collectors.toSet());

        // 3. Gelen listedeki müşterileri bul.
        List<Customer> customersToAdd = customerRepository.findAllById(customerIds);
        if (customersToAdd.size() != customerIds.size()) {
            // Bu, gelen ID'lerden bazılarının geçersiz olduğunu gösterir.
            // Hata fırlatabilir veya görmezden gelebiliriz. Şimdilik devam edelim.
            System.out.println("Warning: Some customer IDs were not found and will be ignored.");
        }

        // 4. Her bir müşteri için, eğer zaten atanmamışsa, yeni bir atama oluştur.
        for (Customer customer : customersToAdd) {
            if (!existingCustomerIds.contains(customer.getId())) {
                RouteAssignment newAssignment = new RouteAssignment();
                newAssignment.setRoute(route);
                newAssignment.setCustomer(customer);
                routeAssignmentRepository.save(newAssignment);
                // Performans için saveAll da kullanılabilir, ancak bu daha net.
            }
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

    @Transactional(readOnly = true)
    public double getTotalDebtForRoute(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));

        return route.getAssignments().stream()
                .map(RouteAssignment::getCustomer)
                .map(Customer::getCurrentBalanceAmount)
                .mapToDouble(java.math.BigDecimal::doubleValue)
                .sum();
    }


}