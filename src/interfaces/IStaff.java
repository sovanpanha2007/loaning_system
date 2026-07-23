package src.interfaces;

import src.model.Contract;
import src.model.Staff;

public interface IStaff {
    
    void setNewName(String name);
    boolean canContractApprove(Staff staff , Contract contract);


}
