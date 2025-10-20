// @ts-nocheck
import React from 'react';
import { Typography } from 'antd';
import { useParams } from 'react-router-dom';

const { Title } = Typography;

const PaymentDetailsPage: React.FC = () => {
  const { id } = useParams();

  return (
    <div>
      <Title level={2}>Payment Details</Title>
      <p>Payment ID: {id}</p>
    </div>
  );
};

export default PaymentDetailsPage;
