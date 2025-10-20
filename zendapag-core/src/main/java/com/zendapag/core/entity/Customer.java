package com.zendapag.core.entity;

import com.zendapag.core.entity.enums.CustomerStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customer_email", columnList = "email"),
    @Index(name = "idx_customer_document", columnList = "document"),
    @Index(name = "idx_customer_phone", columnList = "phone"),
    @Index(name = "idx_customer_merchant", columnList = "merchant_id"),
    @Index(name = "idx_customer_status", columnList = "status"),
    @Index(name = "idx_customer_created_at", columnList = "created_at"),
    @Index(name = "idx_customer_deleted", columnList = "deleted"),
    @Index(name = "idx_customer_composite", columnList = "merchant_id, email")
})
@SQLDelete(sql = "UPDATE customers SET deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
public class Customer extends BaseEntity {

    @NotNull(message = "Merchant is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_customer_merchant"))
    private Merchant merchant;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must be at most 255 characters")
    @Column(name = "email", nullable = false)
    private String email;

    @Pattern(regexp = "^[0-9]{11}$|^[0-9]{14}$", message = "Document must be a valid CPF (11 digits) or CNPJ (14 digits)")
    @Column(name = "document", length = 14)
    private String document;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone must be a valid international format")
    @Size(max = 20, message = "Phone must be at most 20 characters")
    @Column(name = "phone", length = 20)
    private String phone;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private CustomerStatus status;

    // Personal Information
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Size(max = 10, message = "Gender must be at most 10 characters")
    @Column(name = "gender", length = 10)
    private String gender;

    // Address Information
    @Size(max = 10, message = "Postal code must be at most 10 characters")
    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Size(max = 255, message = "Address must be at most 255 characters")
    @Column(name = "address")
    private String address;

    @Size(max = 100, message = "Address number must be at most 100 characters")
    @Column(name = "address_number", length = 100)
    private String addressNumber;

    @Size(max = 255, message = "Address complement must be at most 255 characters")
    @Column(name = "address_complement")
    private String addressComplement;

    @Size(max = 100, message = "Neighborhood must be at most 100 characters")
    @Column(name = "neighborhood", length = 100)
    private String neighborhood;

    @Size(max = 100, message = "City must be at most 100 characters")
    @Column(name = "city", length = 100)
    private String city;

    @Size(max = 100, message = "State must be at most 100 characters")
    @Column(name = "state", length = 100)
    private String state;

    @Size(max = 3, message = "Country code must be at most 3 characters")
    @Column(name = "country", length = 3)
    private String country = "BRA";

    // Preferences
    @Column(name = "email_notifications", nullable = false)
    private Boolean emailNotifications = true;

    @Column(name = "sms_notifications", nullable = false)
    private Boolean smsNotifications = true;

    @Column(name = "marketing_notifications", nullable = false)
    private Boolean marketingNotifications = false;

    // Risk and Verification
    @Column(name = "verified", nullable = false)
    private Boolean verified = false;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Size(max = 2, message = "Language must be 2 characters")
    @Column(name = "language", length = 2)
    private String language = "pt";

    @Size(max = 10, message = "Timezone must be at most 10 characters")
    @Column(name = "timezone", length = 10)
    private String timezone = "UTC-3";

    // Additional Data
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Size(max = 1000, message = "Notes must be at most 1000 characters")
    @Column(name = "notes", length = 1000)
    private String notes;

    // External References
    @Size(max = 255, message = "External ID must be at most 255 characters")
    @Column(name = "external_id")
    private String externalId;

    // Relationships
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Payment> payments = new HashSet<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<PaymentMethod> paymentMethods = new HashSet<>();

    // Constructors
    public Customer() {
        super();
        this.status = CustomerStatus.ACTIVE;
        this.emailNotifications = true;
        this.smsNotifications = true;
        this.marketingNotifications = false;
        this.verified = false;
        this.country = "BRA";
        this.language = "pt";
        this.timezone = "UTC-3";
    }

    public Customer(Merchant merchant, String name, String email) {
        this();
        this.merchant = merchant;
        this.name = name;
        this.email = email;
    }

    // Business Methods
    public boolean isActive() {
        return CustomerStatus.ACTIVE.equals(this.status) && !isDeleted();
    }

    public boolean canMakePayments() {
        return isActive() && !CustomerStatus.BLOCKED.equals(this.status);
    }

    public void activate() {
        this.status = CustomerStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = CustomerStatus.INACTIVE;
    }

    public void block() {
        this.status = CustomerStatus.BLOCKED;
    }

    public void verify() {
        this.verified = true;
        if (CustomerStatus.PENDING_VERIFICATION.equals(this.status)) {
            this.status = CustomerStatus.ACTIVE;
        }
    }

    public boolean isCpf() {
        return document != null && document.length() == 11;
    }

    public boolean isCnpj() {
        return document != null && document.length() == 14;
    }

    public String getFullAddress() {
        StringBuilder fullAddress = new StringBuilder();

        if (address != null) {
            fullAddress.append(address);
        }

        if (addressNumber != null) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(addressNumber);
        }

        if (addressComplement != null) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(addressComplement);
        }

        return fullAddress.toString();
    }

    public void updateMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }

    // Getters and Setters
    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddressNumber() {
        return addressNumber;
    }

    public void setAddressNumber(String addressNumber) {
        this.addressNumber = addressNumber;
    }

    public String getAddressComplement() {
        return addressComplement;
    }

    public void setAddressComplement(String addressComplement) {
        this.addressComplement = addressComplement;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Boolean getEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public Boolean getSmsNotifications() {
        return smsNotifications;
    }

    public void setSmsNotifications(Boolean smsNotifications) {
        this.smsNotifications = smsNotifications;
    }

    public Boolean getMarketingNotifications() {
        return marketingNotifications;
    }

    public void setMarketingNotifications(Boolean marketingNotifications) {
        this.marketingNotifications = marketingNotifications;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Set<Payment> getPayments() {
        return payments;
    }

    public void setPayments(Set<Payment> payments) {
        this.payments = payments;
    }

    public Set<PaymentMethod> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(Set<PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }
}