package src.web.dto;

import java.util.List;

import src.model.PaymentSchedule;

public record PaymentScheduleResponse(int scheduleId, int contractId, boolean fullyPaid, List<PaymentResponse> payments) {

    public static PaymentScheduleResponse from(PaymentSchedule s) {
        return new PaymentScheduleResponse(s.getScheduleId(), s.getContract().getContractId(), s.isFullyPaid(),
                s.getPayments().stream().map(PaymentResponse::from).toList());
    }
}
