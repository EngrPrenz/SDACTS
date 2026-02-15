Imports Microsoft.Data.SqlClient

Public Class MainForm

    Dim connStr As String = "Server=ACER-NITROV15-F\SQLEXPRESS;Database=SampleDB;Trusted_Connection=True;TrustServerCertificate=True;"

    Private Sub MainForm_Load(sender As Object, e As EventArgs) Handles MyBase.Load
        LoadData()
    End Sub

    Private Sub LoadData()
        Using conn As New SqlConnection(connStr)
            conn.Open()

            Dim da As New SqlDataAdapter("SELECT * FROM Products", conn)
            Dim dt As New DataTable
            da.Fill(dt)

            DataGridView1.DataSource = dt
        End Using
    End Sub

    Private Sub btnAdd_Click(sender As Object, e As EventArgs) Handles btnAdd.Click
        Using conn As New SqlConnection(connStr)
            conn.Open()

            Dim query As String = "INSERT INTO Products (Name, Price) VALUES (@name, @price)"

            Using cmd As New SqlCommand(query, conn)
                cmd.Parameters.AddWithValue("@name", txtName.Text)
                cmd.Parameters.AddWithValue("@price", txtPrice.Text)
                cmd.ExecuteNonQuery()
            End Using
        End Using

        LoadData()
        ClearFields()
        MessageBox.Show("Product Added")
    End Sub

    Private Sub btnUpdate_Click(sender As Object, e As EventArgs) Handles btnUpdate.Click
        Using conn As New SqlConnection(connStr)
            conn.Open()

            Dim query As String = "UPDATE Products SET Name=@name, Price=@price WHERE Id=@id"

            Using cmd As New SqlCommand(query, conn)
                cmd.Parameters.AddWithValue("@name", txtName.Text)
                cmd.Parameters.AddWithValue("@price", txtPrice.Text)
                cmd.Parameters.AddWithValue("@id", txtId.Text)
                cmd.ExecuteNonQuery()
            End Using
        End Using

        LoadData()
        ClearFields()
        MessageBox.Show("Product Updated")
    End Sub

    Private Sub btnDelete_Click(sender As Object, e As EventArgs) Handles btnDelete.Click
        Using conn As New SqlConnection(connStr)
            conn.Open()

            Dim query As String = "DELETE FROM Products WHERE Id=@id"

            Using cmd As New SqlCommand(query, conn)
                cmd.Parameters.AddWithValue("@id", txtId.Text)
                cmd.ExecuteNonQuery()
            End Using
        End Using

        LoadData()
        ClearFields()
        MessageBox.Show("Product Deleted")
    End Sub

    Private Sub btnSearch_Click(sender As Object, e As EventArgs) Handles btnSearch.Click
        Using conn As New SqlConnection(connStr)
            conn.Open()

            Dim query As String = "SELECT * FROM Products WHERE Name LIKE @name"

            Using cmd As New SqlCommand(query, conn)
                cmd.Parameters.AddWithValue("@name", "%" & txtName.Text & "%")

                Dim da As New SqlDataAdapter(cmd)
                Dim dt As New DataTable
                da.Fill(dt)

                DataGridView1.DataSource = dt
            End Using
        End Using
    End Sub

    Private Sub DataGridView1_CellClick(sender As Object, e As DataGridViewCellEventArgs) Handles DataGridView1.CellClick
        If e.RowIndex >= 0 Then
            Dim row As DataGridViewRow = DataGridView1.Rows(e.RowIndex)

            txtId.Text = row.Cells("Id").Value.ToString()
            txtName.Text = row.Cells("Name").Value.ToString()
            txtPrice.Text = row.Cells("Price").Value.ToString()
        End If
    End Sub

    Private Sub btnClear_Click(sender As Object, e As EventArgs) Handles btnClear.Click
        ClearFields()
    End Sub

    Private Sub ClearFields()
        txtId.Clear()
        txtName.Clear()
        txtPrice.Clear()
    End Sub

End Class
