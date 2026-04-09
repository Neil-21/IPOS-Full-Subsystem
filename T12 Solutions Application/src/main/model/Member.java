package main.model;

public class Member {
   private String email;
    private String password;
    private boolean account_type;

    public Member(String email, String password, boolean account_type) {
        this.email = email;
        this.password = password;
        this.account_type = account_type;
    }

    public String getEmail(){
        return email;
    }

    public String getPassword(){
        return password;
    }

    public boolean getAccount_type(){
        return account_type;
    }

    public void ChangeEmail(String email){
        this.email = email;
    }

    public void ChangePassword(String password){
        this.password = password;
    }

    public void ChangeaccountType(boolean account_type){
        this.account_type = account_type;
    }

}
