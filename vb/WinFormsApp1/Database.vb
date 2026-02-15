Imports Microsoft.Data.SqlClient

Public Module Database

    Public ConnectionString As String =
        "Server=ACER-NITROV15-F\SQLEXPRESS;Database=SampleDB;Trusted_Connection=True;TrustServerCertificate=True;"

    Public Function GetConnection() As SqlConnection
        Return New SqlConnection(ConnectionString)
    End Function

End Module
