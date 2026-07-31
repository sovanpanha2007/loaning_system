package src.web.dto;

import java.math.BigDecimal;
import java.util.List;

import src.model.Contract;
import src.model.Staff;

public record ContractResponse(
        int id, int applicantId, String applicantName,
        BigDecimal principalAmount, BigDecimal totalAmount, int duration, double interestRate, BigDecimal effectiveApr,
        String status,
        Integer approvingOfficerId, String approvingOfficerName,
        Integer draftingOfficerId, String draftingOfficerName,
        List<Integer> coSignerIds, int committeeVoteCount) {

    public static ContractResponse from(Contract c) {
        Staff approving = c.getApprovingOfficer();
        Staff drafting = c.getDraftingOfficer();
        return new ContractResponse(
                c.getContractId(), c.getApplicant().getId(), c.getApplicant().getName(),
                c.getPrincipalAmount(), c.getTotalAmount(), c.getDuration(), c.getInterestRate(), c.getEffectiveApr(),
                c.getStatus(),
                approving != null ? approving.getId() : null, approving != null ? approving.getName() : null,
                drafting != null ? drafting.getId() : null, drafting != null ? drafting.getName() : null,
                c.getCoSigners().stream().map(Staff::getId).toList(),
                c.getCommitteeVoteCount());
    }
}
