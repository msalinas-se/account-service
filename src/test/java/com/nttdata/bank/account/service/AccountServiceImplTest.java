package com.nttdata.bank.account.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.nttdata.bank.account.client.CreditClient;
import com.nttdata.bank.account.client.CustomerClient;
import com.nttdata.bank.account.client.dto.CustomerDto;
import com.nttdata.bank.account.config.AccountProperties;
import com.nttdata.bank.account.config.AccountProperties.ProductRule;
import com.nttdata.bank.account.dto.AccountRequest;
import com.nttdata.bank.account.dto.AccountUpdateRequest;
import com.nttdata.bank.account.exception.BusinessRuleException;
import com.nttdata.bank.account.exception.ResourceNotFoundException;
import com.nttdata.bank.account.mapper.AccountMapper;
import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.AccountHolder;
import com.nttdata.bank.account.model.AccountType;
import com.nttdata.bank.account.model.CustomerProfile;
import com.nttdata.bank.account.model.CustomerType;
import com.nttdata.bank.account.model.CustomerDebt;
import com.nttdata.bank.account.repository.AccountRepository;
import com.nttdata.bank.account.repository.CustomerDebtRepository;
import com.nttdata.bank.account.repository.MovementRepository;
import com.nttdata.bank.account.rule.ProfileRuleFactory;
import com.nttdata.bank.account.rule.PymeProfileRule;
import com.nttdata.bank.account.rule.StandardProfileRule;
import com.nttdata.bank.account.rule.VipProfileRule;
import com.nttdata.bank.account.service.impl.AccountServiceImpl;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private MovementRepository movementRepository;

  @Mock
  private CustomerClient customerClient;

  @Mock
  private CreditClient creditClient;

  @Mock
  private CustomerDebtRepository customerDebtRepository;

  private AccountServiceImpl accountService;

  private final CustomerDto personal =
      new CustomerDto("1", CustomerType.PERSONAL, CustomerProfile.STANDARD, "45678912", "Ana");
  private final CustomerDto vip =
      new CustomerDto("1", CustomerType.PERSONAL, CustomerProfile.VIP, "45678912", "Ana");
  private final CustomerDto business =
      new CustomerDto("2", CustomerType.BUSINESS, CustomerProfile.STANDARD, "20123456789", "Acme");
  private final CustomerDto pyme =
      new CustomerDto("2", CustomerType.BUSINESS, CustomerProfile.PYME, "20123456789", "Acme");
  private final List<AccountHolder> holders = List.of(new AccountHolder("40000001", "Luis"));

  @BeforeEach
  void setUp() {
    AccountProperties properties = new AccountProperties(new BigDecimal("500"), Map.of(
        AccountType.SAVINGS, new ProductRule(BigDecimal.ZERO, 20, null, BigDecimal.ZERO, 10,
            new BigDecimal("2")),
        AccountType.CHECKING, new ProductRule(new BigDecimal("15"), null, null,
            new BigDecimal("100"), 20, BigDecimal.ONE),
        AccountType.FIXED_TERM, new ProductRule(BigDecimal.ZERO, 1, 15, new BigDecimal("1000"),
            1, BigDecimal.ZERO)));
    ProfileRuleFactory factory = new ProfileRuleFactory(List.of(new StandardProfileRule(),
        new VipProfileRule(creditClient, properties), new PymeProfileRule(creditClient)));
    accountService = new AccountServiceImpl(accountRepository, movementRepository,
        customerClient, properties, new AccountMapper(), factory, customerDebtRepository);
  }

  @BeforeEach
  void noDebts() {
    lenient().when(customerDebtRepository.findById(any(String.class)))
        .thenReturn(Maybe.empty());
  }

  private void saveReturnsArgument() {
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
  }

  private Account account() {
    return Account.builder().id("a1").type(AccountType.CHECKING).balance(BigDecimal.TEN)
        .holders(holders).authorizedSigners(List.of()).build();
  }

  @Test
  void createSavingsAccount() {
    when(customerClient.findById("1")).thenReturn(Single.just(personal));
    when(accountRepository.countByCustomerIdAndType("1", AccountType.SAVINGS))
        .thenReturn(Single.just(0L));
    saveReturnsArgument();

    accountService.create(new AccountRequest("1", AccountType.SAVINGS, null, null, null)).test()
        .assertValue(account -> account.monthlyMovementLimit() == 20
            && account.freeTransactions() == 10 && account.holders().size() == 1);
  }

  @Test
  void createFixedTermAccount() {
    when(customerClient.findById("1")).thenReturn(Single.just(personal));
    saveReturnsArgument();

    accountService.create(new AccountRequest("1", AccountType.FIXED_TERM,
            new BigDecimal("1000"), null, null)).test()
        .assertValue(account -> account.balance().intValue() == 1000);
  }

  @Test
  void openingAmountBelowMinimum() {
    when(customerClient.findById("1")).thenReturn(Single.just(personal));

    accountService.create(new AccountRequest("1", AccountType.FIXED_TERM, BigDecimal.TEN, null,
            null)).test()
        .assertError(BusinessRuleException.class);
  }

  @Test
  void personalCustomerCannotHaveTwoSavingsAccounts() {
    when(customerClient.findById("1")).thenReturn(Single.just(personal));
    when(accountRepository.countByCustomerIdAndType("1", AccountType.SAVINGS))
        .thenReturn(Single.just(1L));

    accountService.create(new AccountRequest("1", AccountType.SAVINGS, null, null, null)).test()
        .assertError(BusinessRuleException.class);
  }

  @Test
  void businessCustomerCannotHaveSavingsAccount() {
    when(customerClient.findById("2")).thenReturn(Single.just(business));

    accountService.create(new AccountRequest("2", AccountType.SAVINGS, null, holders, null))
        .test().assertError(BusinessRuleException.class);
  }

  @Test
  void businessAccountNeedsHolder() {
    when(customerClient.findById("2")).thenReturn(Single.just(business));

    accountService.create(new AccountRequest("2", AccountType.CHECKING, new BigDecimal("100"),
            null, null)).test()
        .assertError(BusinessRuleException.class);
  }

  @Test
  void createBusinessCheckingAccount() {
    when(customerClient.findById("2")).thenReturn(Single.just(business));
    saveReturnsArgument();

    accountService.create(new AccountRequest("2", AccountType.CHECKING, new BigDecimal("100"),
            holders, holders)).test()
        .assertValue(account -> account.maintenanceFee().intValue() == 15
            && account.authorizedSigners().size() == 1);
  }

  @Test
  void vipSavingsAccountWithCreditCard() {
    when(customerClient.findById("1")).thenReturn(Single.just(vip));
    when(accountRepository.countByCustomerIdAndType("1", AccountType.SAVINGS))
        .thenReturn(Single.just(0L));
    when(creditClient.hasCreditCard("1")).thenReturn(Single.just(true));
    saveReturnsArgument();

    accountService.create(new AccountRequest("1", AccountType.SAVINGS, new BigDecimal("600"),
            null, null)).test()
        .assertValue(account -> account.minimumDailyAverage().intValue() == 500);
  }

  @Test
  void vipSavingsAccountWithoutCreditCard() {
    when(customerClient.findById("1")).thenReturn(Single.just(vip));
    when(accountRepository.countByCustomerIdAndType("1", AccountType.SAVINGS))
        .thenReturn(Single.just(0L));
    when(creditClient.hasCreditCard("1")).thenReturn(Single.just(false));

    accountService.create(new AccountRequest("1", AccountType.SAVINGS, new BigDecimal("600"),
            null, null)).test()
        .assertError(BusinessRuleException.class);
  }

  @Test
  void vipSavingsAccountBelowMinimumAverage() {
    when(customerClient.findById("1")).thenReturn(Single.just(vip));
    when(accountRepository.countByCustomerIdAndType("1", AccountType.SAVINGS))
        .thenReturn(Single.just(0L));

    accountService.create(new AccountRequest("1", AccountType.SAVINGS, new BigDecimal("100"),
            null, null)).test()
        .assertError(BusinessRuleException.class);
  }

  @Test
  void pymeCheckingAccountWithoutMaintenanceFee() {
    when(customerClient.findById("2")).thenReturn(Single.just(pyme));
    when(creditClient.hasCreditCard("2")).thenReturn(Single.just(true));
    saveReturnsArgument();

    accountService.create(new AccountRequest("2", AccountType.CHECKING, new BigDecimal("100"),
            holders, null)).test()
        .assertValue(account -> account.maintenanceFee().signum() == 0);
  }

  @Test
  void pymeCheckingAccountWithoutCreditCard() {
    when(customerClient.findById("2")).thenReturn(Single.just(pyme));
    when(creditClient.hasCreditCard("2")).thenReturn(Single.just(false));

    accountService.create(new AccountRequest("2", AccountType.CHECKING, new BigDecimal("100"),
            holders, null)).test()
        .assertError(BusinessRuleException.class);
  }

  @Test
  void customerWithOverdueDebtCannotOpenAccount() {
    when(customerClient.findById("1")).thenReturn(Single.just(personal));
    when(customerDebtRepository.findById("1"))
        .thenReturn(Maybe.just(new CustomerDebt("1", true)));

    accountService.create(new AccountRequest("1", AccountType.SAVINGS, null, null, null)).test()
        .assertError(BusinessRuleException.class);
  }

  @Test
  void customerWithPaidDebtCanOpenAccount() {
    when(customerClient.findById("1")).thenReturn(Single.just(personal));
    when(customerDebtRepository.findById("1"))
        .thenReturn(Maybe.just(new CustomerDebt("1", false)));
    when(accountRepository.countByCustomerIdAndType("1", AccountType.SAVINGS))
        .thenReturn(Single.just(0L));
    saveReturnsArgument();

    accountService.create(new AccountRequest("1", AccountType.SAVINGS, null, null, null)).test()
        .assertComplete();
  }

  @Test
  void findAllAccounts() {
    when(accountRepository.findAll()).thenReturn(Flowable.just(account()));

    accountService.findAll(null).test().assertValueCount(1);
  }

  @Test
  void findAccountsByCustomer() {
    when(accountRepository.findByCustomerId("1")).thenReturn(Flowable.just(account()));

    accountService.findAll("1").test().assertValueCount(1);
  }

  @Test
  void findAccountNotFound() {
    when(accountRepository.findById("x")).thenReturn(Maybe.empty());

    accountService.findById("x").test().assertError(ResourceNotFoundException.class);
  }

  @Test
  void updateAccount() {
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(account()));
    saveReturnsArgument();

    accountService.update("a1", new AccountUpdateRequest(holders, holders)).test()
        .assertValue(account -> account.authorizedSigners().size() == 1);
  }

  @Test
  void deleteAccount() {
    Account account = account();
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(account));
    when(movementRepository.deleteByAccountId("a1")).thenReturn(Single.just(0L));
    when(accountRepository.delete(account)).thenReturn(Completable.complete());

    accountService.delete("a1").test().assertComplete();
  }

  @Test
  void accountBalance() {
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(account()));

    accountService.getBalance("a1").test()
        .assertValue(balance -> balance.balance().intValue() == 10);
  }
}
