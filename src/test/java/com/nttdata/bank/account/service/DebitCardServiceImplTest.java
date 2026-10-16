package com.nttdata.bank.account.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nttdata.bank.account.dto.DebitCardRequest;
import com.nttdata.bank.account.dto.MovementRequest;
import com.nttdata.bank.account.exception.BusinessRuleException;
import com.nttdata.bank.account.exception.ResourceNotFoundException;
import com.nttdata.bank.account.mapper.AccountMapper;
import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.DebitCard;
import com.nttdata.bank.account.model.Movement;
import com.nttdata.bank.account.model.MovementType;
import com.nttdata.bank.account.model.CustomerDebt;
import com.nttdata.bank.account.repository.AccountRepository;
import com.nttdata.bank.account.repository.CustomerDebtRepository;
import com.nttdata.bank.account.repository.DebitCardRepository;
import com.nttdata.bank.account.repository.MovementRepository;
import com.nttdata.bank.account.service.impl.DebitCardServiceImpl;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DebitCardServiceImplTest {

  @Mock
  private DebitCardRepository debitCardRepository;

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private MovementRepository movementRepository;

  @Mock
  private CustomerDebtRepository customerDebtRepository;

  private DebitCardServiceImpl debitCardService;

  @BeforeEach
  void setUp() {
    debitCardService = new DebitCardServiceImpl(debitCardRepository, accountRepository,
        movementRepository, new AccountMapper(), customerDebtRepository);
  }

  private Account account(String id, String customerId, String balance) {
    return Account.builder().id(id).customerId(customerId).balance(new BigDecimal(balance))
        .build();
  }

  private DebitCard card() {
    return DebitCard.builder().id("d1").cardNumber("5000").customerId("1").mainAccountId("a1")
        .accountIds(List.of("a1", "a2")).build();
  }

  @Test
  void createDebitCardWithMainAccountFirst() {
    when(customerDebtRepository.findById("1")).thenReturn(Maybe.empty());
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(account("a1", "1", "0")));
    when(accountRepository.findById("a2")).thenReturn(Maybe.just(account("a2", "1", "0")));
    when(debitCardRepository.save(any(DebitCard.class)))
        .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

    debitCardService.create(new DebitCardRequest("1", "a1", List.of("a2", "a1"))).test()
        .assertValue(card -> card.getAccountIds().equals(List.of("a1", "a2"))
            && card.getCardNumber().length() == 16);
  }

  @Test
  void accountsMustBelongToCustomer() {
    when(customerDebtRepository.findById("1")).thenReturn(Maybe.empty());
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(account("a1", "2", "0")));

    debitCardService.create(new DebitCardRequest("1", "a1", null)).test()
        .assertError(BusinessRuleException.class);
  }

  @Test
  void accountNotFound() {
    when(customerDebtRepository.findById("1")).thenReturn(Maybe.empty());
    when(accountRepository.findById("a1")).thenReturn(Maybe.empty());

    debitCardService.create(new DebitCardRequest("1", "a1", null)).test()
        .assertError(ResourceNotFoundException.class);
  }

  @Test
  void customerWithOverdueDebtCannotGetDebitCard() {
    when(customerDebtRepository.findById("1"))
        .thenReturn(Maybe.just(new CustomerDebt("1", true)));

    debitCardService.create(new DebitCardRequest("1", "a1", null)).test()
        .assertError(BusinessRuleException.class);
  }

  @Test
  void payFromMainAccount() {
    when(debitCardRepository.findById("d1")).thenReturn(Maybe.just(card()));
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(account("a1", "1", "100")));
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
    when(movementRepository.save(any(Movement.class)))
        .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

    debitCardService.pay("d1", new MovementRequest(new BigDecimal("30"))).test()
        .assertValue(movement -> movement.accountId().equals("a1")
            && movement.type() == MovementType.DEBIT_CARD_PAYMENT);
  }

  @Test
  void payFromNextAccountWhenMainHasNoBalance() {
    when(debitCardRepository.findById("d1")).thenReturn(Maybe.just(card()));
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(account("a1", "1", "10")));
    when(accountRepository.findById("a2")).thenReturn(Maybe.just(account("a2", "1", "100")));
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
    when(movementRepository.save(any(Movement.class)))
        .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

    debitCardService.pay("d1", new MovementRequest(new BigDecimal("30"))).test()
        .assertValue(movement -> movement.accountId().equals("a2")
            && movement.balance().intValue() == 70);
  }

  @Test
  void payWithoutBalanceInAnyAccount() {
    when(debitCardRepository.findById("d1")).thenReturn(Maybe.just(card()));
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(account("a1", "1", "10")));
    when(accountRepository.findById("a2")).thenReturn(Maybe.just(account("a2", "1", "5")));

    debitCardService.pay("d1", new MovementRequest(new BigDecimal("30"))).test()
        .assertError(BusinessRuleException.class);
  }

  @Test
  void findAllAndByCustomer() {
    when(debitCardRepository.findAll()).thenReturn(Flowable.just(card()));
    when(debitCardRepository.findByCustomerId("1")).thenReturn(Flowable.just(card()));

    debitCardService.findAll(null).test().assertValueCount(1);
    debitCardService.findAll("1").test().assertValueCount(1);
  }

  @Test
  void updateDebitCard() {
    when(debitCardRepository.findById("d1")).thenReturn(Maybe.just(card()));
    when(accountRepository.findById("a2")).thenReturn(Maybe.just(account("a2", "1", "0")));
    when(debitCardRepository.save(any(DebitCard.class)))
        .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

    debitCardService.update("d1", new DebitCardRequest("1", "a2", null)).test()
        .assertValue(card -> card.getMainAccountId().equals("a2"));
  }

  @Test
  void deleteDebitCard() {
    DebitCard card = card();
    when(debitCardRepository.findById("d1")).thenReturn(Maybe.just(card));
    when(debitCardRepository.delete(card)).thenReturn(Completable.complete());

    debitCardService.delete("d1").test().assertComplete();
  }

  @Test
  void balanceOfMainAccount() {
    when(debitCardRepository.findById("d1")).thenReturn(Maybe.just(card()));
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(account("a1", "1", "80")));

    debitCardService.getBalance("d1").test()
        .assertValue(balance -> balance.balance().intValue() == 80);
  }

  @Test
  void lastMovementsOfMainAccount() {
    when(debitCardRepository.findById("d1")).thenReturn(Maybe.just(card()));
    when(movementRepository.findTop10ByAccountIdOrderByDateDesc("a1"))
        .thenReturn(Flowable.just(new Movement(), new Movement()));

    debitCardService.lastMovements("d1").test().assertValueCount(2);
  }

  @Test
  void debitCardNotFound() {
    when(debitCardRepository.findById("x")).thenReturn(Maybe.empty());

    debitCardService.findById("x").test().assertError(ResourceNotFoundException.class);
  }
}
