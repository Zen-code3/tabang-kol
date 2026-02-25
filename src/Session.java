public class Session {
    public static Integer currentCustomerId = null;
    public static String currentCustomerName = null;
    public static String currentCustomerEmail = null;
    public static boolean isAdmin = false;

    public static void setCustomer(Database.Customer customer) {
        if (customer == null) {
            clear();
            return;
        }
        currentCustomerId = customer.customerId;
        currentCustomerName = customer.fullName;
        currentCustomerEmail = customer.email;
        isAdmin = customer.isAdmin;
    }

    public static void clear() {
        currentCustomerId = null;
        currentCustomerName = null;
        currentCustomerEmail = null;
        isAdmin = false;
    }
}

