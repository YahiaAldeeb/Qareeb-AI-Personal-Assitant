"""
Pydantic schema for financial transaction records.
"""

from typing import Optional
from pydantic import BaseModel, Field


class FinanceRecord(BaseModel):
    """Schema for financial transaction records."""
    transactionID: Optional[int] = Field(None, description="Transaction ID")
    amount: Optional[float] = Field(None, description="Transaction amount")
    date: Optional[str] = Field(None, description="Transaction date in ISO format (YYYY-MM-DD)")
    source: Optional[str] = Field(None, description="Source of the transaction")
    description: Optional[str] = Field(None, description="Description of the transaction")
    income: Optional[bool] = Field(None, description="True if it's income/revenue, False if it's an expense")
    state: Optional[str] = Field(None, description="State/status of the transaction")
    category_name: Optional[str] = Field(None, description="Category name (e.g., 'Food', 'Transport', 'Salary')")
    user_ID: Optional[int] = Field(None, description="User ID associated with the transaction")
    created_at: Optional[str] = Field(None, description="Creation date in ISO format (YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS)")
    is_deleted: Optional[bool] = Field(None, description="True if the transaction is deleted, False otherwise")
