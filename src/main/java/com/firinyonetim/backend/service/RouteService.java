// src/main/java/com/firinyonetim/backend/service/RouteService.java
package com.firinyonetim.backend.service;

import com.firinyonetim.backend.dto.customer.response.CustomerResponse;
import com.firinyonetim.backend.dto.driver.response.DriverCustomerResponse;
import com.firinyonetim.backend.dto.route.RouteSummaryDto;
import com.firinyonetim.backend.dto.route.request.RouteCreateRequest;
import com.firinyonetim.backend.dto.route.response.RouteResponse;
import com.firinyonetim.backend.entity.*;
import com.firinyonetim.backend.exception.ResourceNotFoundException;
import com.firinyonetim.backend.mapper.CustomerMapper;
import com.firinyonetim.backend.mapper.DriverCustomerMapper;
import com.firinyonetim.backend.mapper.RouteMapper;
import com.firinyonetim.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.firinyonetim.backend.dto.route.request.RouteUpdateRequest;
import com.firinyonetim.backend.dto.route.RouteDailySummaryDto;
import com.firinyonetim.backend.entity.ItemType;
import com.firinyonetim.backend.entity.PaymentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.firinyonetim.backend.dto.route.RouteProductSummaryDto;
import com.firinyonetim.backend.entity.TransactionItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final RouteAssignmentRepository routeAssignmentRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final RouteMapper routeMapper;
    private final CustomerMapper customerMapper;
    private final DriverCustomerMapper driverCustomerMapper;
    private final TransactionRepository transactionRepository;
    private static final Logger logger = LoggerFactory.getLogger(RouteService.class);

    // ... createRoute, getAllRoutes ve diğer metotlar aynı kalacak ...
    @Transactional
    public RouteResponse createRoute(RouteCreateRequest request) {
        if (routeRepository.existsByRouteCode(request.getRouteCode())) {
            throw new IllegalStateException("Rota kodu '" + request.getRouteCode() + "' zaten kullanılıyor.");
        }

        Route route = routeMapper.toRoute(request);

        if (request.getDriverId() != null) {
            User driver = userRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getDriverId()));
            if (driver.getRole() != Role.SOFOR) {
                throw new IllegalArgumentException("Bu kullanıcı bir şoför değil.");
            }
            route.setDriver(driver);
        }

        return routeMapper.toRouteResponse(routeRepository.save(route));
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> getAllRoutes() {
        List<RouteSummaryDto> summaries = routeRepository.findAllWithSummary();
        return summaries.stream()
                .map(routeMapper::toRouteResponseFromSummary)
                .collect(Collectors.toList());
    }


    @Transactional
    public void deleteRouteByStatus(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));
        route.setActive(false);
        routeRepository.save(route);
    }

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

        List<RouteAssignment> existingAssignments = routeAssignmentRepository.findByRouteIdOrderByDeliveryOrderAsc(routeId);
        Set<Long> existingCustomerIds = existingAssignments.stream()
                .map(assignment -> assignment.getCustomer().getId())
                .collect(Collectors.toSet());

        Set<Long> desiredCustomerIds = new java.util.HashSet<>(customerIds);

        List<RouteAssignment> assignmentsToRemove = existingAssignments.stream()
                .filter(assignment -> !desiredCustomerIds.contains(assignment.getCustomer().getId()))
                .collect(Collectors.toList());

        if (!assignmentsToRemove.isEmpty()) {
            routeAssignmentRepository.deleteAll(assignmentsToRemove);
        }

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
        routeAssignmentRepository.findByRouteIdOrderByDeliveryOrderAsc(routeId).stream()
                .filter(assignment -> assignment.getCustomer().getId().equals(customerId))
                .findFirst()
                .ifPresent(routeAssignmentRepository::delete);
    }

    public List<CustomerResponse> getCustomersByRoute(Long routeId) {
        return routeAssignmentRepository.findByRouteIdOrderByDeliveryOrderAsc(routeId).stream()
                .map(RouteAssignment::getCustomer)
                .map(customerMapper::toCustomerResponse)
                .collect(Collectors.toList());
    }

    // --- GÜNCELLEME BURADA ---
    @Transactional(readOnly = true)
    public List<DriverCustomerResponse> getCustomersByRouteForDriver(Long routeId) {
        List<Customer> customers = routeAssignmentRepository.findByRouteIdOrderByDeliveryOrderAsc(routeId).stream()
                .map(RouteAssignment::getCustomer)
                .collect(Collectors.toList());

        // Bu satır, Hibernate'i çalışma günleri koleksiyonunu veritabanından yüklemeye zorlar.
        customers.forEach(c -> c.getWorkingDays().size());

        return customers.stream()
                .map(driverCustomerMapper::toDto)
                .collect(Collectors.toList());
    }
    // --- GÜNCELLEME SONU ---

    public List<RouteResponse> getRoutesByCustomer(Long customerId) {
        return routeAssignmentRepository.findByCustomerId(customerId).stream()
                .map(RouteAssignment::getRoute)
                .map(routeMapper::toRouteResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RouteResponse updateRoute(Long routeId, RouteUpdateRequest request) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));

        routeMapper.updateRouteFromDto(request, route);

        if (request.getDriverId() != null) {
            User driver = userRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getDriverId()));
            if (driver.getRole() != Role.SOFOR) {
                throw new IllegalArgumentException("Bu kullanıcı bir şoför değil.");
            }
            route.setDriver(driver);
        } else {
            route.setDriver(null);
        }

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
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));

        List<Long> existingCustomerIds = routeAssignmentRepository.findByRouteIdOrderByDeliveryOrderAsc(routeId).stream()
                .map(assignment -> assignment.getCustomer().getId())
                .collect(Collectors.toList());

        List<Long> newCustomerIds = customerIds.stream()
                .filter(id -> !existingCustomerIds.contains(id))
                .collect(Collectors.toList());

        List<Long> mergedCustomerIds = Stream.concat(existingCustomerIds.stream(), newCustomerIds.stream())
                .distinct()
                .collect(Collectors.toList());

        if (!mergedCustomerIds.isEmpty()) {
            List<Customer> customersToAdd = customerRepository.findAllById(mergedCustomerIds);

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
    public void updateCustomerRoutes(Long customerId, List<Long> routeIds) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        routeAssignmentRepository.deleteByCustomerId(customerId);

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

    @Transactional
    public void addCustomersToRoute(Long routeId, List<Long> customerIds) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));

        Set<Long> existingCustomerIds = route.getAssignments().stream()
                .map(assignment -> assignment.getCustomer().getId())
                .collect(Collectors.toSet());

        List<Customer> customersToAdd = customerRepository.findAllById(customerIds);
        if (customersToAdd.size() != customerIds.size()) {
            System.out.println("Warning: Some customer IDs were not found and will be ignored.");
        }

        for (Customer customer : customersToAdd) {
            if (!existingCustomerIds.contains(customer.getId())) {
                RouteAssignment newAssignment = new RouteAssignment();
                newAssignment.setRoute(route);
                newAssignment.setCustomer(customer);
                routeAssignmentRepository.save(newAssignment);
            }
        }
    }

    public Map<Long, Long> getCustomerCountsPerRoute() {
        List<Route> routes = routeRepository.findAllWithCustomers();
        return routes.stream()
                .collect(Collectors.toMap(
                        Route::getId,
                        route -> (long) route.getAssignments().size()
                ));
    }

    @Transactional
    public RouteResponse toggleRouteStatus(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + routeId));
        route.setActive(!route.isActive());
        Route updatedRoute = routeRepository.save(route);
        return routeMapper.toRouteResponse(updatedRoute);
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


    @Transactional(readOnly = true)
    public List<RouteDailySummaryDto> getDailySummaries(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime startOfNextDay = date.plusDays(1).atStartOfDay();
        List<Transaction> transactions = transactionRepository.findTransactionsBetween(startOfDay, startOfNextDay);

        List<Transaction> approvedTransactions = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.APPROVED)
                .collect(Collectors.toList());

        Map<Long, RouteDailySummaryDto> summaryMap = new HashMap<>();
        Map<Long, Map<Long, RouteProductSummaryDto>> productSummaryMap = new HashMap<>();

        for (Transaction transaction : approvedTransactions) {
            if (transaction.getRoute() == null) {
                continue;
            }

            Long routeId = transaction.getRoute().getId();
            RouteDailySummaryDto summary = summaryMap.computeIfAbsent(routeId, id -> {
                RouteDailySummaryDto newSummary = new RouteDailySummaryDto();
                newSummary.setRouteId(id);
                newSummary.setRouteCode(transaction.getRoute().getRouteCode());
                newSummary.setRouteName(transaction.getRoute().getName());
                return newSummary;
            });

            productSummaryMap.computeIfAbsent(routeId, k -> new HashMap<>());

            for (TransactionItem item : transaction.getItems()) {
                if (item.getType() == ItemType.SATIS) {
                    summary.setTotalSales(summary.getTotalSales().add(item.getTotalPrice()));
                } else if (item.getType() == ItemType.IADE) {
                    summary.setTotalReturns(summary.getTotalReturns().add(item.getTotalPrice()));
                }

                Long productId = item.getProduct().getId();
                RouteProductSummaryDto productSummary = productSummaryMap.get(routeId)
                        .computeIfAbsent(productId, id -> new RouteProductSummaryDto(id, item.getProduct().getName(), 0, 0));

                if (item.getType() == ItemType.SATIS) {
                    productSummary.setTotalSold(productSummary.getTotalSold() + item.getQuantity());
                } else {
                    productSummary.setTotalReturned(productSummary.getTotalReturned() + item.getQuantity());
                }
            }

            transaction.getPayments().forEach(payment -> {
                if (payment.getType() == PaymentType.NAKIT) {
                    summary.setTotalCashPayment(summary.getTotalCashPayment().add(payment.getAmount()));
                } else if (payment.getType() == PaymentType.KART) {
                    summary.setTotalCardPayment(summary.getTotalCardPayment().add(payment.getAmount()));
                }
            });
        }

        summaryMap.values().forEach(summary -> {
            BigDecimal netRevenue = summary.getTotalSales().subtract(summary.getTotalReturns());
            BigDecimal totalPayments = summary.getTotalCashPayment().add(summary.getTotalCardPayment());
            BigDecimal balanceChange = netRevenue.subtract(totalPayments);

            summary.setNetRevenue(netRevenue);
            summary.setBalanceChange(balanceChange);

            Map<Long, RouteProductSummaryDto> productsForRoute = productSummaryMap.get(summary.getRouteId());
            if (productsForRoute != null) {
                summary.setProductSummaries(new ArrayList<>(productsForRoute.values()));
            }
        });

        return new ArrayList<>(summaryMap.values());
    }

    @Transactional
    public void updateDeliveryOrder(Long routeId, List<Long> orderedCustomerIds) {
        List<RouteAssignment> assignments = routeAssignmentRepository.findByRouteIdOrderByDeliveryOrderAsc(routeId);
        Map<Long, RouteAssignment> assignmentMap = assignments.stream()
                .collect(Collectors.toMap(ra -> ra.getCustomer().getId(), Function.identity()));

        for (int i = 0; i < orderedCustomerIds.size(); i++) {
            Long customerId = orderedCustomerIds.get(i);
            RouteAssignment assignment = assignmentMap.get(customerId);
            if (assignment != null) {
                assignment.setDeliveryOrder(i + 1);
            }
        }
        routeAssignmentRepository.saveAll(assignments);
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> getDriverRoutes(Long driverId) {
        return routeRepository.findByDriverIdAndIsActiveTrue(driverId)
                .stream()
                .map(routeMapper::toRouteResponse)
                .collect(Collectors.toList());
    }
}