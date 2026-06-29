
// 1. Định nghĩa Chiến lược (Strategy) cho từng loại giao dịch
public interface TransferStrategy {
    boolean isMatch(String transferType);

    double calculateFee(double amount);

    void routeTransfer(Account sender, Account receiver, double amount);
}

// 2. Các Implementation cụ thể của từng chiến lược
@Component
public class InternalTransferStrategy implements TransferStrategy {
    @Override
    public boolean isMatch(String transferType) {
        return "INTERNAL".equals(transferType);
    }

    @Override
    public double calculateFee(double amount) {
        return 0;
    }

    @Override
    public void routeTransfer(Account sender, Account receiver, double amount) {
        System.out.println("Processing internal system transfer...");
    }
}

@Component
public class DomesticBankTransferStrategy implements TransferStrategy {
    @Override
    public boolean isMatch(String transferType) {
        return "DOMESTIC_BANK".equals(transferType);
    }

    @Override
    public double calculateFee(double amount) {
        return amount * 0.01;
    }

    @Override
    public void routeTransfer(Account sender, Account receiver, double amount) {
        System.out.println("Connecting to Napas API...");
    }
}

@Component
public class InternationalTransferStrategy implements TransferStrategy {
    @Override
    public boolean isMatch(String transferType) {
        return "INTERNATIONAL".equals(transferType);
    }

    @Override
    public double calculateFee(double amount) {
        return amount * 0.03 + 50000;
    }

    @Override
    public void routeTransfer(Account sender, Account receiver, double amount) {
        System.out.println("Connecting to SWIFT API...");
    }
}

// 3. Định nghĩa Sự kiện (Event) khi giao dịch thành công
public class TransactionSuccessEvent {
    private final Transaction transaction;

    public TransactionSuccessEvent(Transaction transaction) {
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }
}

// 4. Lớp dịch vụ cốt lõi đã được Refactor hoàn toàn sạch sẽ
@Service
public class TransferService {
    private final List<TransferStrategy> strategies;
    private final ApplicationEventPublisher eventPublisher;

    public TransferService(List<TransferStrategy> strategies, ApplicationEventPublisher eventPublisher) {
        this.strategies = strategies;
        this.eventPublisher = eventPublisher;
    }

    public Transaction processTransfer(Account sender, Account receiver, double amount, String transferType) {
        if (sender.getBalance() < amount)
            throw new RuntimeException("Insufficient balance");

        // Tìm kiếm chiến lược phù hợp (Factory mẫu linh hoạt)
        TransferStrategy strategy = strategies.stream()
                .filter(s -> s.isMatch(transferType))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Transfer type not supported"));

        double fee = strategy.calculateFee(amount);
        strategy.routeTransfer(sender, receiver, amount);

        // Logic trừ tiền thực tế thực hiện tại đây...

        Transaction transaction = new Transaction(sender, receiver, amount, fee, "SUCCESS");

        // Phát sự kiện giao dịch thành công - Không can thiệp logic thông báo vào đây
        eventPublisher.publishEvent(new TransactionSuccessEvent(transaction));

        return transaction;
    }
}

// 5. Lớp lắng nghe sự kiện để gửi thông báo (Dễ dàng bật/tắt hoặc thêm thông
// báo mới)
@Component
public class NotificationListener {
    @EventListener
    public void handleTransactionSuccess(TransactionSuccessEvent event) {
        Transaction tx = event.getTransaction();
        // Dễ dàng thay đổi từ SMS sang Push Notification ở đây mà không chạm vào
        // TransferService
        System.out.println("Sending Push Notification to " + tx.getSender().getPhone() + " about transaction...");
    }
}