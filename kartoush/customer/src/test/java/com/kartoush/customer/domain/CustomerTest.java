package com.kartoush.customer.domain;

import com.kartoush.customer.exception.CustomerAddressNotFoundException;
import com.kartoush.platform.types.AddressId;
import com.kartoush.platform.types.CustomerId;
import com.kartoush.platform.types.CustomerStatus;
import com.kartoush.platform.types.Email;
import com.kartoush.platform.types.exception.InvalidEmailException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTest
{
    private static final String CUSTOMER_ID_VALUE = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String ADDRESS_ID_VALUE_ONE = "01BX5ZZKBKACTAV9WEVGEMMVRZ";
    private static final String ADDRESS_ID_VALUE_TWO = "01BX5ZZKBKACTAV9WEVGEMMVS0";
    private static final String UNKNOWN_ADDRESS_ID_VALUE = "01BX5ZZKBKACTAV9WEVGEMMVS1";

    private static final Email EMAIL = new Email("jack@kartoush.test");
    private static final String EMAIL_WITH_MIXED_CASE_AND_SPACES = "  Jack@Kartoush.Test  ";
    private static final Email NORMALIZED_EMAIL = new Email("jack@kartoush.test");
    private static final Email UPDATED_EMAIL = new Email("updated@kartoush.test");
    private static final String BLANK_EMAIL = "   ";

    private static final String PASSWORD_HASH = "hashed-password";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Kartoush";
    private static final String PHONE_NUMBER = "3125550100";

    private static final String UPDATED_FIRST_NAME = "John";
    private static final String UPDATED_LAST_NAME = "Customer";
    private static final String UPDATED_PHONE_NUMBER = "6305550100";

    private static final String CUSTOMER_DELETED_MESSAGE = "customer is deleted";
    private static final String EMAIL_BLANK_MESSAGE = "Email must not be blank";
    private static final String EMAIL_NULL_MESSAGE = "Email must not be null";
    private static final String PROFILE_NULL_MESSAGE = "Profile must not be null";
    private static final String PASSWORD_HASH_NULL_MESSAGE = "passwordHash must not be null";
    private static final String ADDRESSES_NULL_MESSAGE = "Addresses must not be null";
    private static final String ADDRESS_NOT_FOUND_PREFIX = "Address not found for id: ";

    private static final CustomerId CUSTOMER_ID = CustomerId.of(CUSTOMER_ID_VALUE);
    private static final AddressId ADDRESS_ID_ONE = AddressId.of(ADDRESS_ID_VALUE_ONE);
    private static final AddressId ADDRESS_ID_TWO = AddressId.of(ADDRESS_ID_VALUE_TWO);
    private static final AddressId UNKNOWN_ADDRESS_ID = AddressId.of(UNKNOWN_ADDRESS_ID_VALUE);

    private static final String ADDRESS_LABEL = "Home";
    private static final String ADDRESS_LINE1 = "123 Test Street";
    private static final String ADDRESS_LINE2 = "Unit 4B";
    private static final String ADDRESS_CITY = "Chicago";
    private static final String ADDRESS_STATE = "IL";
    private static final String ADDRESS_POSTAL_CODE = "60601";
    private static final String ADDRESS_COUNTRY_CODE = "US";

    private static final CustomerProfile PROFILE = new CustomerProfile(
            FIRST_NAME,
            LAST_NAME,
            PHONE_NUMBER);

    private static final CustomerProfile UPDATED_PROFILE = new CustomerProfile(
            UPDATED_FIRST_NAME,
            UPDATED_LAST_NAME,
            UPDATED_PHONE_NUMBER);

    @Test
    void shouldCreateNewCustomer() {
        // when
        final Customer customer = Customer.createNew(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH);

        // then
        assertThat(customer.getId()).isEqualTo(CUSTOMER_ID);
        assertThat(customer.getProfile()).isEqualTo(PROFILE);
        assertThat(customer.getEmail()).isEqualTo(EMAIL);
        assertThat(customer.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.getAddresses()).isEmpty();
    }

    @Test
    void shouldNormalizeEmailWhenCreatingCustomer() {
        // when
        final Customer customer = Customer.createNew(
                CUSTOMER_ID,
                PROFILE,
                new Email(EMAIL_WITH_MIXED_CASE_AND_SPACES),
                PASSWORD_HASH);

        // then
        assertThat(customer.getEmail()).isEqualTo(NORMALIZED_EMAIL);
    }

    @Test
    void shouldThrowWhenCreatingCustomerWithBlankEmail() {
        assertThatThrownBy(() -> Customer.createNew(
                CUSTOMER_ID,
                PROFILE,
                new Email(BLANK_EMAIL),
                PASSWORD_HASH))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessage(EMAIL_BLANK_MESSAGE);
    }

    @Test
    void shouldThrowWhenCreatingCustomerWithNullPasswordHash() {
        assertThatThrownBy(() -> Customer.createNew(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(PASSWORD_HASH_NULL_MESSAGE);
    }

    @Test
    void shouldUpdateDetails() {
        // given
        final Customer customer = Customer.createNew(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH);

        // when
        customer.updateDetails(UPDATED_PROFILE, UPDATED_EMAIL);

        // then
        assertThat(customer.getProfile()).isEqualTo(UPDATED_PROFILE);
        assertThat(customer.getEmail()).isEqualTo(UPDATED_EMAIL);
    }

    @Test
    void shouldNormalizeEmailWhenUpdatingDetails() {
        // given
        final Customer customer = Customer.createNew(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH);

        // when
        customer.updateDetails(UPDATED_PROFILE, new Email("  Updated@Kartoush.Test  "));

        // then
        assertThat(customer.getEmail()).isEqualTo(UPDATED_EMAIL);
    }

    @Test
    void shouldThrowWhenUpdatingDetailsWithNullProfile() {
        // given
        final Customer customer = Customer.createNew(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH);

        // when / then
        assertThatThrownBy(() -> customer.updateDetails(null, UPDATED_EMAIL))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(PROFILE_NULL_MESSAGE);
    }

    @Test
    void shouldThrowWhenUpdatingDetailsWithBlankEmail() {
        // given
        final Customer customer = Customer.createNew(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH);

        // when / then
        assertThatThrownBy(() -> customer.updateDetails(UPDATED_PROFILE, new Email(BLANK_EMAIL)))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessage(EMAIL_BLANK_MESSAGE);
    }

    @Test
    void shouldUpdateStatus() {
        // given
        final Customer customer = Customer.createNew(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH);

        // when
        customer.markDeleted();

        // then
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.DELETED);
    }

    @Test
    void shouldMarkCustomerDeleted() {
        // given
        final Customer customer = Customer.createNew(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH);

        // when
        customer.markDeleted();

        // then
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.DELETED);
    }

    @Test
    void shouldKeepDeletedCustomerDeletedWhenMarkedDeletedAgain() {
        // given
        final Customer customer = Customer.fromPersistence(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH,
                CustomerStatus.DELETED,
                List.of());

        // when
        customer.markDeleted();

        // then
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.DELETED);
    }

    @Test
    void shouldThrowWhenUpdatingDetailsForDeletedCustomer() {
        // given
        final Customer customer = deletedCustomer();

        // when / then
        assertThatThrownBy(() -> customer.updateDetails(UPDATED_PROFILE, UPDATED_EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(CUSTOMER_DELETED_MESSAGE);
    }

    @Test
    void shouldThrowWhenCreatingCustomerWithNullEmail() {
        assertThatThrownBy(() -> Customer.createNew(
                CUSTOMER_ID,
                PROFILE,
                null,
                PASSWORD_HASH))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(EMAIL_NULL_MESSAGE);
    }

    @Test
    void shouldAddAddress() {
        // given
        final Customer customer = Customer.createNew(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH);
        final CustomerAddress address = address(
                ADDRESS_ID_ONE,
                false,
                false);

        // when
        customer.addAddress(address);

        // then
        assertThat(customer.getAddresses()).containsExactly(address);
    }

    @Test
    void shouldThrowWhenAddingNullAddress() {
        // given
        final Customer customer = Customer.createNew(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH);

        // when / then
        assertThatThrownBy(() -> customer.addAddress(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(ADDRESSES_NULL_MESSAGE);
    }

    @Test
    void shouldThrowWhenAddingAddressToDeletedCustomer() {
        // given
        final Customer customer = deletedCustomer();

        // when / then
        assertThatThrownBy(() -> customer.addAddress(address(ADDRESS_ID_ONE, false, false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(CUSTOMER_DELETED_MESSAGE);
    }

    @Test
    void shouldClearExistingDefaultShippingWhenAddingNewDefaultShippingAddress() {
        // given
        final CustomerAddress firstAddress = address(ADDRESS_ID_ONE, true, false);
        final CustomerAddress secondAddress = address(ADDRESS_ID_TWO, true, false);
        final Customer customer = Customer.fromPersistence(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH,
                CustomerStatus.ACTIVE,
                List.of(firstAddress));

        // when
        customer.addAddress(secondAddress);

        // then
        assertThat(firstAddress.isDefaultShipping()).isFalse();
        assertThat(secondAddress.isDefaultShipping()).isTrue();
        assertThat(customer.getAddresses()).containsExactly(firstAddress, secondAddress);
    }

    @Test
    void shouldClearExistingDefaultBillingWhenAddingNewDefaultBillingAddress() {
        // given
        final CustomerAddress firstAddress = address(ADDRESS_ID_ONE, false, true);
        final CustomerAddress secondAddress = address(ADDRESS_ID_TWO, false, true);
        final Customer customer = Customer.fromPersistence(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH,
                CustomerStatus.ACTIVE,
                List.of(firstAddress));

        // when
        customer.addAddress(secondAddress);

        // then
        assertThat(firstAddress.isDefaultBilling()).isFalse();
        assertThat(secondAddress.isDefaultBilling()).isTrue();
        assertThat(customer.getAddresses()).containsExactly(firstAddress, secondAddress);
    }

    @Test
    void shouldSetDefaultShippingAddress() {
        // given
        final CustomerAddress firstAddress = address(ADDRESS_ID_ONE, true, false);
        final CustomerAddress secondAddress = address(ADDRESS_ID_TWO, false, false);
        final Customer customer = Customer.fromPersistence(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH,
                CustomerStatus.ACTIVE,
                List.of(firstAddress, secondAddress));

        // when
        customer.setDefaultShippingAddress(ADDRESS_ID_TWO);

        // then
        assertThat(firstAddress.isDefaultShipping()).isFalse();
        assertThat(secondAddress.isDefaultShipping()).isTrue();
    }

    @Test
    void shouldThrowWhenSettingDefaultShippingAddressThatDoesNotExist() {
        // given
        final Customer customer = Customer.fromPersistence(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH,
                CustomerStatus.ACTIVE,
                List.of(address(ADDRESS_ID_ONE, false, false)));

        // when / then
        assertThatThrownBy(() -> customer.setDefaultShippingAddress(UNKNOWN_ADDRESS_ID))
                .isInstanceOf(CustomerAddressNotFoundException.class)
                .hasMessage(ADDRESS_NOT_FOUND_PREFIX + UNKNOWN_ADDRESS_ID_VALUE);
    }

    @Test
    void shouldThrowWhenSettingDefaultShippingAddressForDeletedCustomer() {
        // given
        final Customer customer = deletedCustomer();

        // when / then
        assertThatThrownBy(() -> customer.setDefaultShippingAddress(ADDRESS_ID_ONE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(CUSTOMER_DELETED_MESSAGE);
    }

    @Test
    void shouldSetDefaultBillingAddress() {
        // given
        final CustomerAddress firstAddress = address(ADDRESS_ID_ONE, false, true);
        final CustomerAddress secondAddress = address(ADDRESS_ID_TWO, false, false);
        final Customer customer = Customer.fromPersistence(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH,
                CustomerStatus.ACTIVE,
                List.of(firstAddress, secondAddress));

        // when
        customer.setDefaultBillingAddress(ADDRESS_ID_TWO);

        // then
        assertThat(firstAddress.isDefaultBilling()).isFalse();
        assertThat(secondAddress.isDefaultBilling()).isTrue();
    }

    @Test
    void shouldThrowWhenSettingDefaultBillingAddressThatDoesNotExist() {
        // given
        final Customer customer = Customer.fromPersistence(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH,
                CustomerStatus.ACTIVE,
                List.of(address(ADDRESS_ID_ONE, false, false)));

        // when / then
        assertThatThrownBy(() -> customer.setDefaultBillingAddress(UNKNOWN_ADDRESS_ID))
                .isInstanceOf(CustomerAddressNotFoundException.class)
                .hasMessage(ADDRESS_NOT_FOUND_PREFIX + UNKNOWN_ADDRESS_ID_VALUE);
    }

    @Test
    void shouldThrowWhenSettingDefaultBillingAddressForDeletedCustomer() {
        // given
        final Customer customer = deletedCustomer();

        // when / then
        assertThatThrownBy(() -> customer.setDefaultBillingAddress(ADDRESS_ID_ONE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(CUSTOMER_DELETED_MESSAGE);
    }

    @Test
    void shouldRemoveAddress() {
        // given
        final CustomerAddress firstAddress = address(ADDRESS_ID_ONE, false, false);
        final CustomerAddress secondAddress = address(ADDRESS_ID_TWO, false, false);
        final Customer customer = Customer.fromPersistence(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH,
                CustomerStatus.ACTIVE,
                List.of(firstAddress, secondAddress));

        // when
        customer.removeAddress(ADDRESS_ID_ONE);

        // then
        assertThat(customer.getAddresses()).containsExactly(secondAddress);
    }

    @Test
    void shouldThrowWhenRemovingAddressThatDoesNotExist() {
        // given
        final Customer customer = Customer.fromPersistence(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH,
                CustomerStatus.ACTIVE,
                List.of(address(ADDRESS_ID_ONE, false, false)));

        // when / then
        assertThatThrownBy(() -> customer.removeAddress(UNKNOWN_ADDRESS_ID))
                .isInstanceOf(CustomerAddressNotFoundException.class)
                .hasMessage(ADDRESS_NOT_FOUND_PREFIX + UNKNOWN_ADDRESS_ID_VALUE);
    }

    @Test
    void shouldThrowWhenRemovingAddressForDeletedCustomer() {
        // given
        final Customer customer = deletedCustomer();

        // when / then
        assertThatThrownBy(() -> customer.removeAddress(ADDRESS_ID_ONE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(CUSTOMER_DELETED_MESSAGE);
    }

    @Test
    void shouldReturnUnmodifiableCopyOfAddresses() {
        // given
        final CustomerAddress address = address(ADDRESS_ID_ONE, false, false);
        final Customer customer = Customer.fromPersistence(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH,
                CustomerStatus.ACTIVE,
                List.of(address));

        // when
        final List<CustomerAddress> addresses = customer.getAddresses();

        // then
        assertThatThrownBy(() -> {
            addresses.add(address(ADDRESS_ID_TWO, false, false));
        })
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldCreateCustomerFromPersistence() {
        // given
        final CustomerAddress address = address(ADDRESS_ID_ONE, true, true);

        // when
        final Customer customer = Customer.fromPersistence(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH,
                CustomerStatus.DELETED,
                List.of(address));

        // then
        assertThat(customer.getId()).isEqualTo(CUSTOMER_ID);
        assertThat(customer.getProfile()).isEqualTo(PROFILE);
        assertThat(customer.getEmail()).isEqualTo(EMAIL);
        assertThat(customer.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.DELETED);
        assertThat(customer.getAddresses()).containsExactly(address);
    }

    @Test
    void shouldThrowWhenCreatingCustomerFromPersistenceWithNullAddresses() {
        assertThatThrownBy(() -> Customer.fromPersistence(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH,
                CustomerStatus.ACTIVE,
                null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(ADDRESSES_NULL_MESSAGE);
    }

    private static Customer deletedCustomer() {
        return Customer.fromPersistence(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH,
                CustomerStatus.DELETED,
                List.of());
    }

    private static CustomerAddress address(
            final AddressId addressId,
            final boolean defaultShipping,
            final boolean defaultBilling) {

        return CustomerAddress.fromPersistence(
                addressId,
                ADDRESS_LABEL,
                ADDRESS_LINE1,
                ADDRESS_LINE2,
                ADDRESS_CITY,
                ADDRESS_STATE,
                ADDRESS_POSTAL_CODE,
                ADDRESS_COUNTRY_CODE,
                defaultShipping,
                defaultBilling);
    }
    @Test
    void shouldReactivateDeletedCustomer() {
        final Customer customer = Customer.fromPersistence(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH,
                CustomerStatus.DELETED,
                List.of());

        customer.reactivate();

        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void shouldKeepActiveCustomerActiveWhenReactivated() {
        final Customer customer = Customer.createNew(
                CUSTOMER_ID,
                PROFILE,
                EMAIL,
                PASSWORD_HASH);

        customer.reactivate();

        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }
}
