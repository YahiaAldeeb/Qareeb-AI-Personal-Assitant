"""
Pydantic schema for task tracking records.
"""

from typing import Optional
from pydantic import BaseModel, Field


class TaskRecord(BaseModel):
    """Schema for task tracking records."""
    taskID: Optional[int] = Field(None, description="Task ID")
    user_ID: Optional[int] = Field(None, description="User ID associated with the task")
    title: Optional[str] = Field(None, description="Task title")
    description: Optional[str] = Field(None, description="Task description")
    status: Optional[str] = Field(None, description="Task status (e.g., 'pending', 'in_progress', 'completed')")
    progressPercentage: Optional[int] = Field(None, ge=0, le=100, description="Progress percentage (0-100)")
    priority: Optional[str] = Field(None, description="Task priority (e.g., 'low', 'medium', 'high')")
    dueDate: Optional[str] = Field(None, description="Due date in ISO format (YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS)")
    created_at: Optional[str] = Field(None, description="Creation date in ISO format (YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS)")
    updated_at: Optional[str] = Field(None, description="Last update date in ISO format (YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS)")
    is_deleted: Optional[bool] = Field(None, description="True if the task is deleted, False otherwise")