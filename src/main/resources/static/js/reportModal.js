$('#reportModal').on('show.bs.modal', function (event) {
  const button = $(event.relatedTarget);
  const userId = button.data('user-id');
  const reportType = button.data('type');
  const targetId = button.data('target-id');

  $('#reportUserId').val(userId);
  $('#reportType').val(reportType);
  $('#reportTargetId').val(targetId);
});

$('#reportForm').on('submit', function (e) {
  e.preventDefault();

  $.ajax({
    type: 'POST',
    url: '/report',
    data: $(this).serialize(),
    success: function () {
      alert('신고가 접수되었습니다.');
      $('#reportModal').modal('hide');
      $('#reportForm')[0].reset();
    },
    error: function () {
      alert('신고 처리 중 오류가 발생했습니다.');
    }
  });
});